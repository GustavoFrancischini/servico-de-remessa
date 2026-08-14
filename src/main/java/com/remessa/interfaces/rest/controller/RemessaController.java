package com.remessa.interfaces.rest.controller;

import com.remessa.application.service.RemessaService;
import com.remessa.domain.model.Transfer;
import com.remessa.interfaces.rest.dto.CreateRemessaRequest;
import com.remessa.interfaces.rest.dto.RemessaResponse;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;
import jakarta.validation.Valid;

@Controller("/api/remessas")
public class RemessaController {

    private final RemessaService remessaService;

    public RemessaController(RemessaService remessaService) {
        this.remessaService = remessaService;
    }

    @Post
    @Status(HttpStatus.CREATED)
    @ExecuteOn(TaskExecutors.BLOCKING)
    public RemessaResponse criar(@Valid @Body CreateRemessaRequest request) {
        Transfer transfer = remessaService.executar(
                request.senderId(),
                request.receiverId(),
                request.amountBrl());
        return RemessaResponse.from(transfer);
    }
}
