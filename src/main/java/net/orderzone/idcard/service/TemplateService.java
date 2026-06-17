package net.orderzone.idcard.service;

import net.orderzone.idcard.model.Template;
import net.orderzone.idcard.repository.TemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TemplateService {

    private final TemplateRepository templateRepository;

    public TemplateService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public List<Template> getAllTemplates() {
        return templateRepository.findAll();
    }

    public Optional<Template> getTemplateById(Long id) {
        return templateRepository.findById(id);
    }

    public Optional<Template> getTemplateByCode(String code) {
        return templateRepository.findByCode(code);
    }

    public Template saveTemplate(Template template) {
        if (templateRepository.existsByCode(template.getCode())) {
            throw new IllegalArgumentException("Template with code '" + template.getCode() + "' already exists.");
        }
        return templateRepository.save(template);
    }

    public List<Template> searchTemplates(String query) {
        if (query == null || query.isBlank()) {
            return templateRepository.findAll();
        }
        return templateRepository.searchTemplates(query);
    }

    public Template getOrCreateDefaultTemplate() {
        return templateRepository.findByCode("DEFAULT")
                .orElseGet(() -> templateRepository.save(Template.builder()
                        .code("DEFAULT")
                        .name("Standard Classic Theme")
                        .organizationName("OrderZone Academy")
                        .layout("VERTICAL")
                        .primaryColor("#1d4ed8")
                        .secondaryColor("#e0e7ff")
                        .textColor("#111827")
                        .tagline("Excellence in Innovation")
                        .build()));
    }
}