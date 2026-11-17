package br.com.cobranca.whatsapp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cobrancas")
public class WhatsappController {

    private final WhatsappService service;

    public WhatsappController(WhatsappService service) {
        this.service = service;
    }

    @GetMapping("/{id}/whatsapp")
    WhatsappService.Mensagem lembrete(@PathVariable long id) {
        return service.lembrete(id);
    }
}
