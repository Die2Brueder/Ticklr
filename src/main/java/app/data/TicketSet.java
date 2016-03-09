package app.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Created by DucNguyenMinh on 07.03.16.
 */
@Entity
@Table(name = "ticket_sets")
public class TicketSet
{
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(name = "price")
    @Min(0)
    protected BigDecimal price;

    @Column(name = "title")
    @NotEmpty
    protected String title;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    protected Event event;

    /**
     * @param title
     * @param price
     */
    public TicketSet(String title, BigDecimal price)
    {
        this.price = price;
        this.title = title;
    }

    protected TicketSet()
    {
    }

    /**
     * @return ID of this ticket set
     */
    public Long getId()
    {
        return id;
    }

    /**
     * @return price of this ticket set
     */
    public BigDecimal getPrice()
    {
        return price;
    }

    /**
     * @param price of this ticket set
     */
    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    /**
     * @return title of this ticket set
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * @param title title of thi ticket set
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * @return the event that this ticket set belongs to
     */
    @JsonIgnore
    public Event getEvent()
    {
        return event;
    }

    /**
     * @param event the event that this ticket set belongs to
     */
    public void setEvent(Event event)
    {
        this.event = event;
    }

    /**
     * @param other
     * @return new TicketSet with attributes of the given TicketSet
     */
    public TicketSet merge(TicketSet other)
    {
        TicketSet ticketSet = new TicketSet(other.getTitle(), other.getPrice());
        ticketSet.id = this.id;
        ticketSet.event = this.event;
        return ticketSet;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TicketSet ticketSet = (TicketSet) o;

        if (price != null ? !price.equals(ticketSet.price) : ticketSet.price != null) return false;
        return title != null ? title.equals(ticketSet.title) : ticketSet.title == null;

    }

    @Override
    public int hashCode()
    {
        int result = price != null ? price.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        return result;
    }
}
