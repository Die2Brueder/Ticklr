package app.web;

import app.data.*;
import app.data.Event.Visibility;
import app.data.validation.EventValidator;
import app.supports.converter.EnumConverter;
import app.web.authorization.UserAuthorizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;

@RestController
@RequestMapping("/users/{userId}/events")
public class EventController
{
    private static final ResponseEntity NOT_FOUND = new ResponseEntity(HttpStatus.NOT_FOUND);
    private static final ResponseEntity FORBIDDEN = new ResponseEntity(HttpStatus.FORBIDDEN);

    private EventRepository eventRepository;

    private UserRepository userRepository;

    private EventValidator validator;

    private TicketSetRepository ticketSetRepository;

    private UserAuthorizer userAuthorizer;

    /**
     * @param eventRepository Manage Event entities
     * @param validator       performs validation on Event entity
     */
    @Autowired
    public EventController(EventRepository eventRepository, UserRepository userRepository,
                           TicketSetRepository ticketSetRepository, EventValidator validator, UserAuthorizer userAuthorizer)
    {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.ticketSetRepository = ticketSetRepository;
        this.validator = validator;
        this.userAuthorizer = userAuthorizer;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        binder.registerCustomEditor(Visibility.class, new EnumConverter(Visibility.class));
    }

    /**
     * @param requestEvent
     * @param bindingResult
     * @return
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity createEvent(@PathVariable Long userId, Event requestEvent, BindingResult bindingResult)
    {
        User user = userRepository.findById(userId);

        if (user == null)
            return NOT_FOUND;

        if (!userAuthorizer.authorize(user))
            return FORBIDDEN;

        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.CREATED;

        validator.validate(requestEvent, bindingResult);

        if (!bindingResult.hasFieldErrors()) {

            requestEvent.setUser(user);

            Event event = eventRepository.saveOrUpdate(requestEvent);

            headers.setLocation(URI.create("/users/" + userId + "/events/" + event.getId()));
        } else {
            status = HttpStatus.BAD_REQUEST;
        }

        return new ResponseEntity(bindingResult.getFieldErrors(), headers, status);
    }

    /**
     * @param requestEvent
     * @param bindingResult
     * @return
     */
    @RequestMapping(value = "/{eventId}", method = RequestMethod.PUT)
    public ResponseEntity updateEvent(@PathVariable Long userId, @PathVariable Long eventId, Event requestEvent, BindingResult bindingResult)
    {
        Event event = eventRepository.findById(eventId);

        if (event == null)
            return NOT_FOUND;

        User user = event.getUser();
        if (!user.getId().equals(userId)) // event doesn't belong to user
            return NOT_FOUND;

        if (!userAuthorizer.authorize(user))
            return FORBIDDEN;

        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.NO_CONTENT;

        validator.validate(requestEvent, bindingResult);

        if (!bindingResult.hasFieldErrors()) {

            Event updatedEvent = eventRepository.saveOrUpdate(event.merge(requestEvent));

            headers.setLocation(URI.create("/users/" + userId + "/events/" + updatedEvent.getId()));
        } else {
            status = HttpStatus.BAD_REQUEST;
        }

        return new ResponseEntity(bindingResult.getFieldErrors(), headers, status);
    }

    @RequestMapping(value = "/{eventId}", method = RequestMethod.GET)
    public ResponseEntity showEvent(@PathVariable Long userId, @PathVariable Long eventId)
    {
        User user = userRepository.findById(userId);

        if (user == null)
            return NOT_FOUND;

        Event event = eventRepository.findById(eventId);

        if (event == null || !event.getUser().getId().equals(userId))
            return NOT_FOUND;

        if (!userAuthorizer.authorize(user))
            return FORBIDDEN;

        return new ResponseEntity(event, HttpStatus.OK);
    }

    @RequestMapping(value = "/{eventId}", method = RequestMethod.DELETE)
    public ResponseEntity cancelEvent(@PathVariable Long userId, @PathVariable Long eventId)
    {
        User user = userRepository.findById(userId);

        if (user == null)
            return NOT_FOUND;

        Event event = eventRepository.findById(eventId);

        // no event with the given eventId belongs to user with userId found
        if (event == null || !event.getUser().getId().equals(userId))
            return NOT_FOUND;

        if (!userAuthorizer.authorize(user))
            return FORBIDDEN;

        event.setCanceled(true);

        eventRepository.saveOrUpdate(event);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/{eventId}/ticket-sets", method = RequestMethod.POST)
    public ResponseEntity addTicketSet(@PathVariable Long userId, @PathVariable Long eventId, @Valid TicketSet ticketSet, BindingResult bindingResult)
    {
        User user = userRepository.findById(userId);

        if (user == null)
            return NOT_FOUND;

        Event event = eventRepository.findById(eventId);

        // no event with the given eventId belongs to user with userId found
        if (event == null || !event.getUser().getId().equals(userId))
            return NOT_FOUND;

        if (!userAuthorizer.authorize(user))
            return FORBIDDEN;

        if (bindingResult.hasFieldErrors())
            return new ResponseEntity(bindingResult.getFieldErrors(), HttpStatus.BAD_REQUEST);

        event.addTicketSet(ticketSet);

        eventRepository.saveOrUpdate(event);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/users/" + userId + "/events/" + event.getId() + "/ticket-sets/" + ticketSet.getId()));

        return new ResponseEntity(headers, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{eventId}/ticket-sets/{ticketSetId}", method = RequestMethod.PUT)
    public ResponseEntity updateTicketSet(@PathVariable long userId, @PathVariable long eventId,
                                          @PathVariable long ticketSetId, @Valid TicketSet updatedTicketSet, BindingResult bindingResult)
    {
        TicketSet ticketSet = ticketSetRepository.findById(ticketSetId);

        if (ticketSet == null)
            return NOT_FOUND;

        Event event = ticketSet.getEvent();
        if (!event.getId().equals(eventId)) // ticketSet doesn't belong to event
            return NOT_FOUND;

        User user = event.getUser();
        if (!user.getId().equals(userId)) // event doesn't belong to user
            return NOT_FOUND;

        if (!userAuthorizer.authorize(user))
            return FORBIDDEN;

        if (bindingResult.hasFieldErrors())
            return new ResponseEntity(bindingResult.getFieldErrors(), HttpStatus.BAD_REQUEST);

        ticketSetRepository.saveOrUpdate(ticketSet.merge(updatedTicketSet));

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    // todo deals with situation when basket items still reference this ticket set
    @RequestMapping(value = "/{eventId}/ticket-sets/{ticketSetId}", method = RequestMethod.DELETE)
    public ResponseEntity deleteTicketSet(@PathVariable long userId, @PathVariable long eventId, @PathVariable long ticketSetId)
    {
        TicketSet ticketSet = ticketSetRepository.findById(ticketSetId);

        if (ticketSet == null)
            return NOT_FOUND;

        Event event = ticketSet.getEvent();
        if (!event.getId().equals(eventId)) // ticketSet doesn't belong to event
            return NOT_FOUND;

        User user = event.getUser();
        if (!user.getId().equals(userId)) // event doesn't belong to user
            return NOT_FOUND;

        if (!userAuthorizer.authorize(user))
            return FORBIDDEN;

        ticketSetRepository.delete(ticketSet);

        return new ResponseEntity(HttpStatus.OK);
    }
}
