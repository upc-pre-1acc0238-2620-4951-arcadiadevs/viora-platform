package com.arcadiadevs.viora.platform.assistant.application.queryservices;

import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetQuestionCatalogQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenQuestionCatalog;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

/**
 * Application query service port for the question catalog of the assistant.
 */
public interface QuestionCatalogQueryService {

    /**
     * Handles the retrieval of the questions of a screen.
     *
     * @param query query criteria
     * @return the evaluated catalog or application error
     */
    Result<ScreenQuestionCatalog, ApplicationError> handle(GetQuestionCatalogQuery query);
}
