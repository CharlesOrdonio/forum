import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.*;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@SpringBootApplication
public class ForumHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(ForumHubApplication.class, args);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

@RestController
@RequestMapping("/api/topics")
class TopicController {
    private final TopicRepository topicRepository;

    public TopicController(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @PostMapping
    public Topic createTopic(@Valid @RequestBody Topic topic) {
        return topicRepository.save(topic);
    }

    @GetMapping
    public List<Topic> getAllTopics() {
        return topicRepository.findAll();
    }

    @GetMapping("/{id}")
    public Topic getTopicById(@PathVariable Long id) {
        return topicRepository.findById(id).orElseThrow(() -> new TopicNotFoundException(id));
    }

    @PutMapping("/{id}")
    public Topic updateTopic(@PathVariable Long id, @Valid @RequestBody Topic updatedTopic) {
        return topicRepository.findById(id).map(topic -> {
            topic.setTitle(updatedTopic.getTitle());
            topic.setContent(updatedTopic.getContent());
            return topicRepository.save(topic);
        }).orElseThrow(() -> new TopicNotFoundException(id));
    }

    @DeleteMapping("/{id}")
    public void deleteTopic(@PathVariable Long id) {
        if (!topicRepository.existsById(id)) {
            throw new TopicNotFoundException(id);
        }
        topicRepository.deleteById(id);
    }

    /**
     * Método para "não mostrar" um tópico específico.
     * Marca o tópico como oculto sem removê-lo do banco de dados.
     */
    @PatchMapping("/{id}/hide")
    public Topic hideTopic(@PathVariable Long id) {
        return topicRepository.findById(id).map(topic -> {
            topic.setHidden(true); // Marca o tópico como oculto
            return topicRepository.save(topic);
        }).orElseThrow(() -> new TopicNotFoundException(id));
    }
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class TopicNotFoundException extends RuntimeException {
    public TopicNotFoundException(Long id) {
        super("Tópico não encontrado com o ID " + id);
    }
}

@Entity
class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Título é obrigatório")
    private String title;

    @NotBlank(message = "Conteúdo é obrigatório")
    private String content;

    private boolean hidden = false; // Flag para indicar se o tópico está oculto

    // Getters e setters omitidos para brevidade
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}

interface TopicRepository extends JpaRepository<Topic, Long> {}

@Configuration
class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/api/**").authenticated()
                .and()
            .httpBasic();

        return http.build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, PasswordEncoder passwordEncoder) throws Exception {
        auth.inMemoryAuthentication()
            .withUser("user")
            .password(passwordEncoder.encode("password"))
            .roles("USER");
    }
}
