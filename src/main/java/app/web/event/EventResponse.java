package app.web.event;

import app.data.Event;

import java.time.ZonedDateTime;

/**
 * @author ngnmhieu
 * @since 16.05.16
 */
public class EventResponse
{
    public Long id;

    public String href;

    public String title;

    public String description;

    public ZonedDateTime startTime;

    public ZonedDateTime endTime;

    public Event.Visibility visibility;

    public boolean canceled;

    public boolean happening;

    public boolean expired;

    public EventResponse(Event event, EventURI eventUri)
    {
        this.href = eventUri.eventURL(event.getUser().getId(), event.getId());
        this.id = event.getId();
        this.title = event.getTitle();
        this.description = event.getDescription();
        this.startTime = event.getStartTime();
        this.endTime = event.getEndTime();
        this.visibility = event.getVisibility();
        this.canceled = event.isCanceled();
        this.happening = event.isHappening();
        this.expired = event.isExpired();
    }
}
