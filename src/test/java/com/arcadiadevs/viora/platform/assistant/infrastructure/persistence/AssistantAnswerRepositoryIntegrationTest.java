package com.arcadiadevs.viora.platform.assistant.infrastructure.persistence;

import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerAction;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerModel;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerSource;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.repositories.AssistantAnswerRepository;
import com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.entities.AssistantAnswerRequestPersistenceEntity;
import com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.repositories.AssistantAnswerRequestPersistenceRepository;
import com.arcadiadevs.viora.platform.assistant.infrastructure.scheduling.AssistantRetentionScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The JPA adapter against H2: one key per producer across both tables, retention and purge in bulk. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantwriter;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key="
})
@DisplayName("Assistant answers repository (JPA, H2)")
class AssistantAnswerRepositoryIntegrationTest {

    private static final DataVersion VERSION = new DataVersion("sha256:test");
    private static final List<AnswerFact> FACTS = List.of(new AnswerFact("0,57", "Carga", "Alta", FactTone.PRIMARY),
            new AnswerFact("0,40", "Objetivo", "Plan", FactTone.PRIMARY));

    @Autowired AssistantAnswerRepository repository;
    @Autowired AssistantAnswerRequestPersistenceRepository requests;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;

    @Test
    @DisplayName("A key already served from the cache cannot create an answer: the second request rolls back")
    void keyBoundFromTheCacheBlocksANewAnswer() {
        var producer = UUID.randomUUID().toString();
        var cached = repository.save(answer(producer, UUID.randomUUID(), clock.instant()));
        var key = UUID.randomUUID();
        repository.bindRequest(producer, key, cached.id());

        assertThatThrownBy(() -> repository.save(answer(producer, key, clock.instant())))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(repository.findByClientRequestId(producer, key)).map(AssistantAnswer::id).contains(cached.id());
        assertThat(count("select count(*) from assistant_answers where producer_id = ?", producer)).isEqualTo(1);
    }

    @Test
    @DisplayName("A key that created an answer cannot be bound to a cached one")
    void keyOfANewAnswerCannotBeBound() {
        var producer = UUID.randomUUID().toString();
        var cached = repository.save(answer(producer, UUID.randomUUID(), clock.instant()));
        var key = UUID.randomUUID();
        var created = repository.save(answer(producer, key, clock.instant()));

        assertThatThrownBy(() -> repository.bindRequest(producer, key, cached.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(repository.findByClientRequestId(producer, key)).map(AssistantAnswer::id).contains(created.id());
    }

    @Test
    @DisplayName("The daily job deletes answers older than 10 days and their keys, and keeps everything newer")
    void retentionDeletesOnlyOldRows() {
        var producer = UUID.randomUUID().toString();
        var now = clock.instant();
        var old = repository.save(answer(producer, UUID.randomUUID(), now.minus(Duration.ofDays(11))));
        var recent = repository.save(answer(producer, UUID.randomUUID(), now.minus(Duration.ofDays(9))));
        var oldKey = UUID.randomUUID();
        var recentKey = UUID.randomUUID();
        repository.bindRequest(producer, recentKey, recent.id());
        // A recent key served the old answer from the cache: it goes with its answer
        repository.bindRequest(producer, oldKey, old.id());
        var orphan = request(producer, UUID.randomUUID(), now.minus(Duration.ofDays(1)));
        var expiredKey = request(producer, recent.id(), now.minus(Duration.ofDays(12)));

        new AssistantRetentionScheduler(repository, Clock.fixed(now, ZoneOffset.UTC), 10).run();

        assertThat(repository.findById(old.id())).isEmpty();
        assertThat(repository.findById(recent.id())).isPresent();
        assertThat(repository.findByClientRequestId(producer, oldKey)).isEmpty();
        assertThat(repository.findByClientRequestId(producer, recentKey)).map(AssistantAnswer::id)
                .contains(recent.id());
        assertThat(repository.findByClientRequestId(producer, recent.clientRequestId())).isPresent();
        assertThat(requests.findById(orphan)).isEmpty();
        assertThat(requests.findById(expiredKey)).isEmpty();
        assertThat(count("select count(*) from assistant_answer_requests where answer_id = ?", old.id())).isZero();
    }

    @Test
    @DisplayName("The account purge deletes the answers and keys of the producer only")
    void purgeDeletesOneProducer() {
        var producer = UUID.randomUUID().toString();
        var other = UUID.randomUUID().toString();
        var answer = repository.save(answer(producer, UUID.randomUUID(), clock.instant()));
        repository.bindRequest(producer, UUID.randomUUID(), answer.id());
        var kept = repository.save(answer(other, UUID.randomUUID(), clock.instant()));

        repository.deleteAllByProducerId(producer);

        assertThat(count("select count(*) from assistant_answers where producer_id = ?", producer)).isZero();
        assertThat(count("select count(*) from assistant_answer_requests where producer_id = ?", producer)).isZero();
        assertThat(repository.findById(kept.id())).isPresent();
        assertThat(count("select count(*) from assistant_answer_requests where producer_id = ?", other)).isEqualTo(1);
    }

    @Test
    @DisplayName("A whole-field answer is stored without plot and never served for a plot, nor a plot one for the field")
    void fieldAnswersAreKeptApartFromPlotAnswers() {
        var producer = UUID.randomUUID().toString();
        var plot = new PlotId(UUID.randomUUID().toString());
        var question = new QuestionId("home.alerts_affecting");
        var since = clock.instant().minus(Duration.ofHours(1));
        var field = repository.save(answer(producer, UUID.randomUUID(), null, question, clock.instant()));

        assertThat(jdbc.queryForObject("select plot_id from assistant_answers where id = ?", String.class,
                field.id())).isNull();
        assertThat(repository.findById(field.id())).map(AssistantAnswer::plotId).isEmpty();
        assertThat(repository.findLatestCached(producer, question, null, "es", VERSION, since))
                .map(AssistantAnswer::id).contains(field.id());
        assertThat(repository.findLatestCached(producer, question, plot, "es", VERSION, since)).isEmpty();
        assertThat(repository.findLatestOfScreen(producer, null, "home.", since)).map(AssistantAnswer::id)
                .contains(field.id());
        assertThat(repository.findLatestOfScreen(producer, plot, "home.", since)).isEmpty();

        var ofPlot = repository.save(answer(producer, UUID.randomUUID(), plot, question,
                clock.instant().plusSeconds(1)));

        assertThat(repository.findLatestCached(producer, question, null, "es", VERSION, since))
                .map(AssistantAnswer::id).contains(field.id());
        assertThat(repository.findLatestOfScreen(producer, null, "home.", since)).map(AssistantAnswer::id)
                .contains(field.id());
        assertThat(repository.findLatestCached(producer, question, plot, "es", VERSION, since))
                .map(AssistantAnswer::id).contains(ofPlot.id());
        assertThat(repository.findLatestOfScreen(producer, plot, "home.", since)).map(AssistantAnswer::id)
                .contains(ofPlot.id());
        assertThat(repository.findRecent(producer, since, 10)).extracting(AssistantAnswer::id)
                .containsExactly(ofPlot.id(), field.id());
    }

    @Test
    @DisplayName("The job refuses a retention shorter than the 10 days of the history")
    void retentionCoversTheHistory() {
        assertThatThrownBy(() -> new AssistantRetentionScheduler(repository, clock, 9))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private UUID request(String producer, UUID answerId, Instant createdAt) {
        var request = new AssistantAnswerRequestPersistenceEntity();
        request.setId(UUID.randomUUID());
        request.setProducerId(producer);
        request.setClientRequestId(UUID.randomUUID());
        request.setAnswerId(answerId);
        request.setCreatedAt(createdAt);
        return requests.saveAndFlush(request).getId();
    }

    private int count(String sql, Object arg) {
        return jdbc.queryForObject(sql, Integer.class, arg);
    }

    private static AssistantAnswer answer(String producer, UUID key, Instant createdAt) {
        return answer(producer, key, new PlotId(UUID.randomUUID().toString()), QuestionId.PLAN_WHY_PERCENT,
                createdAt);
    }

    private static AssistantAnswer answer(String producer, UUID key, PlotId plot, QuestionId question,
            Instant createdAt) {
        return AssistantAnswer.create(key, producer, plot, question, "es", VERSION, "Texto.",
                FACTS, List.<AnswerSource>of(), List.<AnswerAction>of(), AnswerModel.TEMPLATE, null,
                null, createdAt);
    }
}
