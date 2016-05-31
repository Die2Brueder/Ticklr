package app.services;

import app.data.Basket;
import app.data.BasketItem;
import app.data.TicketSet;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.util.UUID;

/**
 * @author DucNguyenMinh
 * @since 08.03.16
 */
@Repository
@Transactional
public class BasketService
{
    @PersistenceContext
    private EntityManager em;

    public BasketService(EntityManager em)
    {
        this.em = em;
    }

    protected BasketService()
    {
    }

    /**
     * @param buyerId ID of the user who owns the basket
     * @return null if nothing found
     */
    public Basket findByBuyerId(UUID buyerId)
    {
        Query query = em.createQuery("SELECT b FROM Basket b WHERE b.buyer.id = :buyer_id");

        query.setParameter("buyer_id", buyerId);

        Basket basket = null;

        try {
            basket = (Basket) query.getSingleResult();
        } catch (NoResultException e) {
        }

        return basket;
    }

    /**
     * Add a new item corresponding to the given ticket set to the basket
     * If the item is already there, then increment the quantity
     *
     * @param basket
     * @param ticketSet
     * @param quantity
     * @return the new BasketItem
     * @throws IllegalArgumentException if quantity > 0
     * @ensure item.getTicketSet() == ticketSet
     * @ensure item.getUnitPrice().equals(ticketSet.getPrice())
     * @ensure item.getQuantity() == (basket.isInBasket(ticketSet) ? basket.getItem(ticketSet).getQuantity() + quantity : quantity)
     */
    public BasketItem addItemToBasket(Basket basket, TicketSet ticketSet, int quantity)
    {
        Assert.notNull(basket);
        Assert.notNull(ticketSet);
        Assert.isTrue(quantity > 0);

        BasketItem item;
        if (basket.isInBasket(ticketSet)) {
            item = basket.getItemFor(ticketSet);
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            item = new BasketItem(ticketSet, quantity, ticketSet.getPrice());
            basket.addItem(item);
        }

        em.merge(basket);

        return item;
    }

    /**
     * @param basket
     * @return
     */
    public Basket save(Basket basket)
    {
        em.persist(basket);
        em.flush();
        return basket;
    }

    /**
     * @param basket
     * @return
     */
    public Basket saveOrUpdate(Basket basket)
    {
        if (basket.getId() == null) {
            em.persist(basket);
        } else {
            basket = em.merge(basket);
        }

        em.flush();

        return basket;
    }

    /**
     * @param item != null
     */
    public BasketItem updateItem(BasketItem item)
    {
        return em.merge(item);
    }

    /**
     * @param basket
     * @param item
     * @throws IllegalArgumentException !basket.isInBasket(item)
     * @ensures false == basket.isInBasket(item)
     */
    public void removeItem(Basket basket, BasketItem item)
    {
        Assert.isTrue(basket.isInBasket(item));

        // make item entity managed if needed
        item = em.contains(item) ? item : em.merge(item);

        basket.removeItem(item);

        em.remove(item);
    }

    /**
     * @param itemId
     * @return the basket item with the given itemId
     */
    public BasketItem findItemById(long itemId)
    {
        Query query = em.createQuery("SELECT i FROM BasketItem i WHERE i.id = :itemId")
                .setParameter("itemId", itemId);

        try {
            return (BasketItem) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}