package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import com.arcadiadevs.viora.platform.assistant.application.internal.answers.ModelFacts;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

/**
 * The prompt of one answer: fixed instructions, what the question asks and what each fact means as the system
 * message and, as the user message, only the facts as JSON followed by the output format. No text of the producer ever goes in it (closed questions).
 */
final class AnswerPrompt {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final String SYSTEM = """
            You write the answer of Viora, the assistant of a fruit growing app, to one closed question a producer \
            asked about a plot. Voice: a trusted field technician, clear, direct and serene, backed by field data.
            %s
            Rules:
            - Write 2 or 3 short sentences, at most 280 characters in total.
            - Use only the facts of the JSON. Every number you write must be one of the facts, with the same digits \
            and decimals; %s. Never round, convert, add, subtract or compute numbers, and write no other number.
            - You may write a date fact (YYYY-MM-DD) as day and month in words, without the year.
            - Keep every number with the meaning of its field below; never attach it to another field.
            - Say only what the facts say: no causes, risks, trends, sequences or forecasts that no field gives.
            - Answer the question asked, starting with its main fact.
            - Recommend no action other than the labels in allowedActions; when it is empty, recommend none.
            - If irrigationAdvisable is false, never mention irrigating or watering.
            - If a fact says the data source is simulated, say "%s".
            - Never mention questionId, field names or JSON.
            - No medical, legal or financial advice.""";

    private AnswerPrompt() {
    }

    /**
     * Builds the prompt.
     *
     * @param facts  the facts for the model, interpreted facts included
     * @param locale the language of the answer
     * @param format the output format instructions of the converter
     * @return the prompt
     */
    static Prompt of(SortedMap<String, Object> facts, Locale locale, String format) {
        var questionId = facts.get(ModelFacts.QUESTION) == null ? null : facts.get(ModelFacts.QUESTION).toString();
        return new Prompt(List.of(new SystemMessage(system(locale, questionId, facts)),
                new UserMessage(user(facts, format))));
    }

    static String system(Locale locale, String questionId, Map<String, ?> facts) {
        var context = new StringBuilder();
        FactGlossary.question(questionId).ifPresent(question -> context.append("\nQuestion: ").append(question));
        var fields = facts.keySet().stream()
                .flatMap(name -> FactGlossary.fact(name).map(meaning -> "\n- " + name + ": " + meaning).stream())
                .collect(Collectors.joining());
        if (!fields.isEmpty()) {
            context.append("\nFields:").append(fields);
        }
        return base(locale) + context;
    }

    private static String base(Locale locale) {
        boolean spanish = "es".equals(locale.getLanguage());
        return SYSTEM.formatted(
                spanish ? "Write in neutral Spanish and address the producer as \"tú\" (tu lote, tu carga)."
                        : "Write in plain English.",
                spanish ? "write decimals with a comma (0,57)" : "write decimals with a point (0.57)",
                spanish ? "dato simulado" : "simulated data");
    }

    static String user(SortedMap<String, Object> facts, String format) {
        return "Facts:\n" + JSON.writeValueAsString(facts) + "\n\n" + format;
    }
}
