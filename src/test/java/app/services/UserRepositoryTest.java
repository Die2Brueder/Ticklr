package app.services;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import app.data.Identity;
import app.data.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.UUID;

/**
 * @author ngnmhieu
 */
@RunWith(MockitoJUnitRunner.class)
public class UserRepositoryTest
{
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    EntityManager em;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IdentityRepository identityRepository;

    @Mock
    User user;

    UUID userId = UUID.fromString("4eab8080-0f0e-11e6-9f74-0002a5d5c51b");

    protected UserRepository userRepository;

    @Before
    public void setUp()
    {
        userRepository = new UserRepository(em, identityRepository);
    }

    @Test
    public void findById_shouldReturnTheCorrectUser() throws Exception
    {
        when(em.find(User.class, userId)).thenReturn(user);

        assertEquals(user, userRepository.findById(userId));
    }

    @Test
    public void findById_shouldReturnNullIfNoUserFound() throws Exception
    {
        when(em.find(User.class, userId)).thenReturn(null);

        assertNull(userRepository.findById(userId));
    }

    @Test
    public void findByEmail_should_return_user() throws Exception
    {
        when(em.createQuery(anyString())
                .setParameter(anyString(), anyString())
                .getSingleResult()
        ).thenReturn(user);

        UserRepository repo = new UserRepository(em, identityRepository);
        assertEquals(user, repo.findByIdentity(new Identity("email", "password")));
    }

    @Test
    public void findByIdentity_should_return_null_if_nothing_found() throws Exception
    {
        when(em.createQuery(anyString())
                .setParameter(anyString(), anyString())
                .getSingleResult()
        ).thenThrow(NoResultException.class);


        assertNull(userRepository.findByIdentity(new Identity("email", "password")));
    }

    @Test
    public void save_ShouldPersistNewUserAndCreateNewIdentity() throws Exception
    {
        User mockUser = mock(User.class);
        Identity id = mock(Identity.class);
        when(mockUser.getIdentity()).thenReturn(id);

        assertEquals(mockUser, userRepository.save(mockUser));
        verify(em, times(1)).persist(mockUser);
        verify(identityRepository, times(1)).save(id);
    }
}
