package br.com.cobranca.demo;

import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rotas exclusivas da demo: só existem com o profile "demo". */
@RestController
@RequestMapping("/api/demo")
@Profile("demo")
public class DemoController implements ApplicationRunner {

    private final DemoSeedService seed;
    private final DemoPagamentoService pagamentos;

    public DemoController(DemoSeedService seed, DemoPagamentoService pagamentos) {
        this.seed = seed;
        this.pagamentos = pagamentos;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed.semearSeVazio();
    }

    @PostMapping("/resetar")
    Map<String, String> resetar() {
        seed.resetar();
        return Map.of("status", "demo restaurada");
    }

    /** Botão "Simular pagamento (só demo)". Não existe fora do profile "demo". */
    @PostMapping("/cobrancas/{id}/simular-pagamento")
    Map<String, String> simular(@PathVariable long id) {
        return pagamentos.simular(id);
    }
}
