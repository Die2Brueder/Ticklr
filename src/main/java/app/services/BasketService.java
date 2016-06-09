package app.services;

import app.data.basket.Basket;
import app.data.basket.BasketItem;
import app.data.event.TicketSet;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.persistence.*;

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
     * Save the basket to the database
     * @param basket
     * @return updated basket
     * @throws IllegalArgumentException if basket == null
     */
    public Basket saveBasket(Basket basket)
    {
        Assert.notNull(basket);

        if (basket.getId() == null) {
            em.persist(basket);
        } else {
            basket = em.merge(basket);
        }

        return basket;
    }

    /**
     * Update quantity of a BasketItem
     *
     * @param item
     * @param quantity
     * @throws IllegalArgumentException if quantity > 0
     * @throws IllegalArgumentException if item == null
     */
    public void updateItemQuantity(BasketItem item, int quantity)
    {
        Assert.notNull(item);
        Assert.isTrue(quantity > 0);

        item.setQuantity(quantity);

        em.merge(item);
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
}
