package br.com.cobranca.importacao;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/importacoes")
public class ImportacaoController {

    private final CsvImportService service;

    public ImportacaoController(CsvImportService service) {
        this.service = service;
    }

    @PostMapping
    ImportacaoResultado importar(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return service.importar(arquivo.getInputStream(), arquivo.getOriginalFilename());
    }

    @ExceptionHandler(ImportacaoInvalidaException.class)
    ResponseEntity<Map<String, String>> invalida(ImportacaoInvalidaException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", e.getMessage()));
    }
}
