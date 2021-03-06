package app.services;

import app.data.event.TicketSet;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author ngnmhieu
 */
@Repository
@Transactional
public class TicketSetRepository
{
    @PersistenceContext
    private EntityManager em;

    public TicketSetRepository(EntityManager em)
    {
        this.em = em;
    }

    protected TicketSetRepository()
    {
    }

    /**
     * @param id of the ticket set
     * @return
     */
    public TicketSet findById(Long id)
    {
        return em.find(TicketSet.class, id);
    }

    /**
     * @param ticketSet to be saved
     * @return
     */
    public TicketSet saveOrUpdate(TicketSet ticketSet)
    {
        if (ticketSet.getId() == null) {
            em.persist(ticketSet);
        } else {
            ticketSet = em.merge(ticketSet);
        }

        em.flush();

        return ticketSet;
    }

    /**
     * Remove ticket set
     * @param ticketSet
     */
    public void delete(TicketSet ticketSet)
    {
        // make this entity managed if needed
        ticketSet = em.contains(ticketSet) ? ticketSet : em.merge(ticketSet);

        ticketSet.getEvent().removeTicketSet(ticketSet);

        em.remove(ticketSet);
    }
}
