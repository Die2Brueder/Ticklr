package app.integration;

import app.config.RootConfig;
import app.config.WebConfig;
import app.data.User;
import app.web.authentication.JwtAuthenticator;
import org.dbunit.DataSourceBasedDBTestCase;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.flywaydb.test.annotation.FlywayTest;
import org.flywaydb.test.junit.FlywayTestExecutionListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.token.Token;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author ngnmhieu
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RootConfig.class, WebConfig.class})
@WebAppConfiguration("src/main/java")
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        FlywayTestExecutionListener.class
})
@ActiveProfiles("test")
@FlywayTest
public class AuthenticationIT extends DataSourceBasedDBTestCase
{
    @Value("${auth.secret}")
    private String authSecret;

    @Test
    public void shouldLoadTestFixture() throws Exception
    {
        User u = (User) em.createQuery("SELECT u FROM User u WHERE u.email = 'user@example.com'").getSingleResult();
        assertEquals("user@example.com", u.getEmail());
    }

    @Test
    public void shouldRespondWithJWTTokenWhenProvidedWithValidCredential() throws Exception
    {
        mockMvc.perform(
                post("/users/request-auth-token")
                        .param("email", "user@example.com")
                        .param("password", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0.x3iQ7s9QYz40aUxkY7hc2t6sWgyU1sXIrS2AP9CnjJk"));
    }

    @Test
    public void shouldRespondWithJWTTokenWhenProvidedWithInvalidCredential() throws Exception
    {
        mockMvc.perform(
                post("/users/request-auth-token")
                        .param("email", "user@example.com")
                        .param("password", "wrong_pasword"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldRespondWithUnauthroziedWhenProvidedWithNonExistentUser() throws Exception
    {
        String email = "nonexistentuser@example.com";
        long userCount = (long) em.createQuery("SELECT COUNT(u) FROM User u WHERE u.email = :email").setParameter("email", email).getSingleResult();
        assertEquals(0, userCount);

        mockMvc.perform(
                post("/users/request-auth-token")
                        .param("email", email)
                        .param("password", "123456789"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldReturnResourceWhenProvidedWithValidJwtToken() throws Exception
    {
        JwtAuthenticator jwt = new JwtAuthenticator(authSecret);
        Token jwtToken = jwt.generateToken("user@example.com");

        mockMvc.perform(get("/admin")
                .header("Authorization", "Bearer " + jwtToken.getKey()))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldNotReturnResourceWhenProvidedWithoutJwtAuthenticationToken() throws Exception
    {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @PersistenceContext
    EntityManager em;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    DataSource dataSource;

    private MockMvc mockMvc;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
    }

    @After
    @Override
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    @Override
    protected DataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    protected IDataSet getDataSet() throws Exception
    {
        return new FlatXmlDataSetBuilder().build(getClass().getResourceAsStream("/fixtures/user_dataset.xml"));
    }
}
