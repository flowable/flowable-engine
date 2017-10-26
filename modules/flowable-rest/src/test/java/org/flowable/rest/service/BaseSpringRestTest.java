package org.flowable.rest.service;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.common.impl.db.DbSchemaManager;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.rest.DispatcherServletConfiguration;
import org.flowable.rest.conf.ApplicationConfiguration;
import org.flowable.rest.util.FlowableResponseFieldsSnippet;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;

import capital.scalable.restdocs.AutoDocumentation;
import capital.scalable.restdocs.payload.JacksonResponseFieldSnippet;
import capital.scalable.restdocs.response.ResponseModifyingPreprocessors;
import com.fasterxml.jackson.databind.ObjectMapper;
import static capital.scalable.restdocs.jackson.JacksonResultHandlers.prepareJackson;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Filip Hrisafov
 */
@WebAppConfiguration
@ContextConfiguration(classes = {
    ApplicationConfiguration.class,
    BaseSpringRestTest.BaseTestSpringRestConfiguration.class,
    DispatcherServletConfiguration.class
})
@RunWith(SpringRunner.class)
public abstract class BaseSpringRestTest {

    protected static final List<String> TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK = Arrays.asList("ACT_GE_PROPERTY", "ACT_ID_PROPERTY");

    @Configuration
    static class BaseTestSpringRestConfiguration {

        @Bean
        public BaseSpringRestRule restRule(ProcessEngine processEngine) {
            BaseSpringRestRule restRule = new BaseSpringRestRule();
            restRule.setProcessEngine(processEngine);
            return restRule;
        }
    }

    static class MyResponseFieldSnippet extends JacksonResponseFieldSnippet {

        @Override
        protected Type getType(HandlerMethod method) {
            return method.getReturnType().getGenericParameterType();
        }
    }

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Rule
    @Autowired
    public BaseSpringRestRule restRule;

    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

    @Autowired
    private ProcessEngine processEngine;
    @Autowired
    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    @Autowired
    protected RepositoryService repositoryService;
    @Autowired
    protected RuntimeService runtimeService;
    @Autowired
    protected TaskService taskService;
    @Autowired
    protected FormService formService;
    @Autowired
    protected HistoryService historyService;
    @Autowired
    protected IdentityService identityService;
    @Autowired
    protected ManagementService managementService;

    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected WebApplicationContext webApplicationContext;
    protected MockMvc mockMvc;

    @BeforeClass
    public static void setUpBeforeClass() {
        System.setProperty("org.springframework.restdocs.javadocJsonDir","target/generated-javadoc-json,../flowable-common-rest/target/generated-javadoc-json");
    }
    @Before
    public void setUp() {
        createUsers();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .alwaysDo(prepareJackson(objectMapper))
            .alwaysDo(print())
            .apply(documentationConfiguration(restDocumentation)
                .snippets().withAdditionalDefaults(
                    AutoDocumentation.description(),
                    AutoDocumentation.methodAndPath(),
                    AutoDocumentation.pathParameters(),
                    AutoDocumentation.requestParameters(),
                    AutoDocumentation.requestFields(),
                    new FlowableResponseFieldsSnippet(),
                    AutoDocumentation.section()
                ))
            .build();
    }

    protected void createUsers() {
        User user = identityService.newUser("kermit");
        user.setFirstName("Kermit");
        user.setLastName("the Frog");
        user.setPassword("kermit");
        identityService.saveUser(user);

        Group group = identityService.newGroup("admin");
        group.setName("Administrators");
        identityService.saveGroup(group);

        identityService.createMembership(user.getId(), group.getId());
    }

    @After
    public void tearDown() throws Throwable {
        dropUsers();
        assertAndEnsureCleanDb();
        processEngineConfiguration.getClock().reset();
    }

    protected void dropUsers() {
        IdentityService identityService = processEngine.getIdentityService();

        identityService.deleteUser("kermit");
        identityService.deleteGroup("admin");
        identityService.deleteMembership("kermit", "admin");
    }

    /**
     * Each test is assumed to clean up all DB content it entered. After a test method executed, this method scans all tables to see if the DB is completely clean. It throws AssertionFailed in case
     * the DB is not clean. If the DB is not clean, it is cleaned by performing a create a drop.
     */
    protected void assertAndEnsureCleanDb() throws Throwable {
        LOGGER.debug("verifying that db is clean after test");
        Map<String, Long> tableCounts = managementService.getTableCount();
        StringBuilder outputMessage = new StringBuilder();
        for (String tableName : tableCounts.keySet()) {
            String tableNameWithoutPrefix = tableName.replace(processEngineConfiguration.getDatabaseTablePrefix(), "");
            if (!TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.contains(tableNameWithoutPrefix)) {
                Long count = tableCounts.get(tableName);
                if (count != 0L) {
                    outputMessage.append("  ").append(tableName).append(": ").append(count.toString()).append(" record(s) ");
                }
            }
        }
        if (outputMessage.length() > 0) {
            outputMessage.insert(0, "DB NOT CLEAN: \n");
            LOGGER.error("\n");
            LOGGER.error(outputMessage.toString());

            LOGGER.info("dropping and recreating db");

            CommandExecutor commandExecutor = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration().getCommandExecutor();
            commandExecutor.execute(new Command<Object>() {

                @Override
                public Object execute(CommandContext commandContext) {
                    DbSchemaManager dbSchemaManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDbSchemaManager();
                    dbSchemaManager.dbSchemaDrop();
                    dbSchemaManager.dbSchemaCreate();
                    return null;
                }
            });

            if (restRule.getException() != null) {
                throw restRule.getException();
            } else {
                Assert.fail(outputMessage.toString());
            }
        } else {
            LOGGER.info("database was clean");
        }
    }

    /**
     * Checks if the returned "data" array (child-node of root-json node returned by invoking a GET on the given url) contains entries with the given ID's.
     */
    protected void assertResultsPresentInDataResponse(String url, String... expectedResourceIds) throws Exception {
        int numberOfResultsExpected = expectedResourceIds.length;

        // Do the actual call
        mockMvc.perform(get("/" + url))
            // Check status and size
            .andExpect(status().isOk())
            .andDo(
                document("{class-name}/{method-name}/{step}",
                    Preprocessors.preprocessRequest(),
                    Preprocessors.preprocessResponse(
                        ResponseModifyingPreprocessors.replaceBinaryContent(),
                        Preprocessors.prettyPrint()
                    )
                    ))
//            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("data.[*]", hasSize(numberOfResultsExpected)))
            .andExpect(jsonPath("data[*].id", containsInAnyOrder(expectedResourceIds)));
    }
}
