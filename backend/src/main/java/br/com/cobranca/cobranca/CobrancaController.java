package br.com.cobranca.cobranca;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cobrancas")
public class CobrancaController {

    private final CobrancaService service;

    public CobrancaController(CobrancaService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    CobrancaService.Detalhe detalhe(@PathVariable long id) {
        return service.detalhe(id);
    }

    @PostMapping("/{id}/pix")
    CobrancaService.Detalhe gerarPix(@PathVariable long id) {
        return service.gerarPix(id);
    }
}
