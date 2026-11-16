package br.com.cobranca.painel;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/painel")
public class PainelController {

    private final PainelService service;

    public PainelController(PainelService service) {
        this.service = service;
    }

    @GetMapping
    PainelService.Painel painel(@RequestParam(name = "mes", required = false) String mes) {
        try {
            return service.consultar(mes == null ? service.mesAtual() : YearMonth.parse(mes));
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mes deve ser AAAA-MM");
        }
    }
}
