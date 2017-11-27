package au.com.rds.test;

import static org.mockito.Mockito.mock;

import org.flowable.app.repository.editor.ModelHistoryRepository;
import org.flowable.app.repository.editor.ModelRelationRepository;
import org.flowable.app.repository.editor.ModelRepository;
import org.flowable.app.service.editor.ModelImageService;
import org.flowable.app.service.editor.ModelServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import au.com.rds.schemaformbuilder.formdesignjson.FormDesignJsonService;

@Configuration
public class TestContext
{
  @Bean
  ModelServiceImpl modelService()
  {
    return new ModelServiceImpl();
  }

  @Bean
  ModelImageService modelImageService()
  {
    return mock(ModelImageService.class);
  }

  @Bean
  ModelRepository modelRepository()
  {
    return mock(ModelRepository.class);
  }

  @Bean
  ModelHistoryRepository modelHistoryRepository()
  {
    return mock(ModelHistoryRepository.class);
  }

  @Bean
  ModelRelationRepository modelRelationRepository()
  {
    return mock(ModelRelationRepository.class);
  }

  @Bean
  ObjectMapper objectMapper()
  {
    return new ObjectMapper();
  }

  @Bean
  FormDesignJsonService formDesignJsonService()
  {
    return mock(FormDesignJsonService.class);
  }

}
