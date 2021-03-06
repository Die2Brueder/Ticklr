package app.web.event;

import app.data.event.Event;
import app.data.event.TicketSet;
import app.data.user.User;
import app.web.event.responses.*;
import app.web.validation.EventValidator;
import app.services.event.EventRepository;
import app.services.TicketSetRepository;
import app.services.UserService;
import app.web.ResourceURI;
import app.web.authorization.IdentityAuthorizer;
import app.web.common.response.expansion.ResponseExpansion;
import app.web.event.forms.TicketSetForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
public class EventController
{
    private static final ResponseEntity NOT_FOUND = new ResponseEntity(HttpStatus.NOT_FOUND);

    private static final ResponseEntity FORBIDDEN = new ResponseEntity(HttpStatus.FORBIDDEN);

    private ResourceURI resURI;

    private EventURI eventURI;

    private EventRepository eventRepository;

    private UserService userService;

    private EventValidator validator;

    private TicketSetRepository ticketSetRepository;

    private IdentityAuthorizer identityAuthorizer;

    /**
     * @param eventRepository     manages event entities
     * @param userService         manages user entities
     * @param ticketSetRepository manages ticketset entities
     * @param validator           validates event input
     * @param identityAuthorizer
     * @param resURI
     */
    @Autowired
    public EventController(EventRepository eventRepository, UserService userService,
                           TicketSetRepository ticketSetRepository, EventValidator validator,
                           IdentityAuthorizer identityAuthorizer, ResourceURI resURI)
    {
        this.eventRepository = eventRepository;
        this.userService = userService;
        this.ticketSetRepository = ticketSetRepository;
        this.validator = validator;
        this.identityAuthorizer = identityAuthorizer;
        this.resURI = resURI;
        this.eventURI = resURI.getEventURI();
    }

    /**
     * @return List of events belonging to a particular user
     */
    @RequestMapping(value = EventURI.EVENTS_URI, method = RequestMethod.GET)
    public ResponseEntity getEvents(@PathVariable UUID userId, @RequestParam(name = "expand", required = false) String expand)
    {
        User user = userService.findById(userId);

        if (user == null)
            return NOT_FOUND;

        if (!identityAuthorizer.authorize(user.getIdentity()))
            return FORBIDDEN;

        List<Event> events = eventRepository.findByUserId(userId);

        Object res = ResponseExpansion.expand(new EventsResponse(user, events, resURI), getExpansionRules(expand));

        return new ResponseEntity(res, HttpStatus.OK);
    }

    /**
     * @param requestEvent
     * @param bindingResult
     * @return
     */
    @RequestMapping(value = EventURI.EVENTS_URI, method = RequestMethod.POST)
    public ResponseEntity createEvent(@PathVariable UUID userId, @Valid @RequestBody(required = false) Event requestEvent, BindingResult bindingResult)
    {
        User user = userService.findById(userId);

        if (user == null)
            return NOT_FOUND;

        if (!identityAuthorizer.authorize(user.getIdentity()))
            return FORBIDDEN;

        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.CREATED;

        if (requestEvent == null)
            requestEvent = new Event();

        validator.validate(requestEvent, bindingResult);

        if (!bindingResult.hasFieldErrors()) {

            requestEvent.setUser(user);

            Event event = eventRepository.save(requestEvent);

            headers.setLocation(URI.create(eventURI.eventURL(userId, event.getId())));
        } else {
            status = HttpStatus.BAD_REQUEST;
        }

        return new ResponseEntity(bindingResult.getFieldErrors(), headers, status);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity handleHttpMessageNotReadableException(HttpMessageNotReadableException ex)
    {
        // TODO: use logger to log ex.getMessage()
        return new ResponseEntity("{\"message\": \"The request sent by the client was syntactically incorrect.\"}", HttpStatus.BAD_REQUEST);
    }

    /**
     * @param requestEvent
     * @param bindingResult
     * @return
     */
    @RequestMapping(value = EventURI.EVENT_URI, method = RequestMethod.PUT)
    public ResponseEntity updateEvent(@PathVariable UUID userId, @PathVariable Long eventId, @Valid @RequestBody(required = false) Event requestEvent, BindingResult bindingResult)
    {
        Event event = eventRepository.findById(eventId);

        if (event == null)
            return NOT_FOUND;

        User user = event.getUser();
        // Event doesn't belong to user
        if (!user.getId().equals(userId))
            return NOT_FOUND;

        if (!identityAuthorizer.authorize(user.getIdentity()))
            return FORBIDDEN;

        if (requestEvent == null)
            throw new HttpMessageNotReadableException("Request body must be provided");

        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.NO_CONTENT;

        validator.validate(requestEvent, bindingResult);

        if (!bindingResult.hasFieldErrors()) {
            Event updatedEvent = eventRepository.save(event.merge(requestEvent));
            headers.setLocation(URI.create(eventURI.eventURL(userId, updatedEvent.getId())));
        } else {
            status = HttpStatus.BAD_REQUEST;
        }

        return new ResponseEntity(bindingResult.getFieldErrors(), headers, status);
    }

    @RequestMapping(value = EventURI.EVENT_URI, method = RequestMethod.GET)
    public ResponseEntity showEvent(@PathVariable UUID userId, @PathVariable Long eventId, @RequestParam(name = "expand", required = false) String expand)
    {
        User user = userService.findById(userId);

        if (user == null)
            return NOT_FOUND;

        Event event = eventRepository.findById(eventId);

        if (event == null || !event.getUser().getId().equals(userId))
            return NOT_FOUND;

        if (!identityAuthorizer.authorize(user.getIdentity()))
            return FORBIDDEN;

        EventResponse eventResponse = new EventResponse(event, resURI, new TicketSetsResponse(event, resURI));

        Object response = ResponseExpansion.expand(eventResponse, getExpansionRules(expand));

        return new ResponseEntity(response, HttpStatus.OK);
    }

    @RequestMapping(value = EventURI.PUBLIC_EVENT_URI, method = RequestMethod.GET)
    public ResponseEntity showPublicEvent(@PathVariable Long eventId, @RequestParam(name = "expand", required = false) String expand)
    {
        Event event = eventRepository.findById(eventId);

        if (!event.isPublic())
            return NOT_FOUND;

        EventResponse eventResponse = new EventResponse(event, resURI, new TicketSetsResponse(event, resURI));

        Object response = ResponseExpansion.expand(eventResponse, getExpansionRules(expand));

        return new ResponseEntity(response, HttpStatus.OK);
    }

    @RequestMapping(value = EventURI.PUBLIC_EVENTS_URI, method = RequestMethod.GET)
    public ResponseEntity showPublicEvents(@RequestParam(name = "expand", required = false) String expand)
    {
        List<Event> events = eventRepository.findPublicEvents();

        PublicEventsResponse eventsResponse = new PublicEventsResponse(events, resURI);

        Object response = ResponseExpansion.expand(eventsResponse, getExpansionRules(expand));

        return new ResponseEntity(response, HttpStatus.OK);
    }

    @RequestMapping(value = EventURI.EVENT_URI, method = RequestMethod.DELETE)
    public ResponseEntity cancelEvent(@PathVariable UUID userId, @PathVariable Long eventId)
    {
        User user = userService.findById(userId);

        if (user == null)
            return NOT_FOUND;

        Event event = eventRepository.findById(eventId);

        // no event with the given eventId belongs to user with userId found
        if (event == null || !event.getUser().getId().equals(userId))
            return NOT_FOUND;

        if (!identityAuthorizer.authorize(user.getIdentity()))
            return FORBIDDEN;

        event.setCanceled(true);

        eventRepository.save(event);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = EventURI.TICKET_SETS_URI, method = RequestMethod.GET)
    public ResponseEntity getTicketSets(@PathVariable UUID userId, @PathVariable Long eventId,
                                        @RequestParam(name = "expand", required = false) String expand)
    {
        User user = userService.findById(userId);

        if (user == null)
            return NOT_FOUND;

        Event event = eventRepository.findById(eventId);

        // no event with the given eventId belongs to user with userId found
        if (event == null || !event.getUser().getId().equals(userId))
            return NOT_FOUND;

        if (!identityAuthorizer.authorize(user.getIdentity()))
            return FORBIDDEN;

        TicketSetsResponse ticketSetsResponse = new TicketSetsResponse(event, resURI);

        Object res = ResponseExpansion.expand(ticketSetsResponse, getExpansionRules(expand));

        return new ResponseEntity(res, HttpStatus.OK);
    }


    @RequestMapping(value = EventURI.TICKET_SET_URI, method = RequestMethod.GET)
    public ResponseEntity getTicketSet(@PathVariable UUID userId, @PathVariable long eventId,
                                       @PathVariable long ticketSetId, @RequestParam(name = "expand", required = false) String expand)
    {
        TicketSet ticketSet = ticketSetRepository.findById(ticketSetId);

        if (ticketSet == null)
            return NOT_FOUND;

        Event event = ticketSet.getEvent();
        // ticketSet doesn't belong to event
        if (!event.getId().equals(eventId))
            return NOT_FOUND;

        User user = event.getUser();
        // event doesn't belong to user
        if (!user.getId().equals(userId))
            return NOT_FOUND;

        if (!identityAuthorizer.authorize(user.getIdentity()))
            return FORBIDDEN;

        TicketSetResponse ticketSetResponse = new TicketSetResponse(ticketSet, resURI, new EventResponse(event, resURI));

        Object res = ResponseExpansion.expand(ticketSetResponse, getExpansionRules(expand));

        return new ResponseEntity(res, HttpStatus.OK);
    }

    @RequestMapping(value = EventURI.TICKET_SETS_URI, method = RequestMethod.POST)
    public ResponseEntity addTicketSet(@PathVariable UUID userId, @PathVariable Long eventId,
                                       @RequestBody(required = false) @Valid TicketSetForm ticketSetForm,
                                       BindingResult bindingResult)
    {
        User user = userService.findById(userId);

        if (user == null)
            return NOT_FOUND;

        Event event = eventRepository.findById(eventId);

        // no event with the given eventId belongs to user with userId found
        if (event == null || !event.getUser().getId().equals(userId))
            return NOT_FOUND;

        if (!identityAuthorizer.authorize(user.getIdentity()))
            return FORBIDDEN;

        if (ticketSetForm == null)
            throw new HttpMessageNotReadableException("Request body must be provided");

        if (bindingResult.hasFieldErrors())
            return new ResponseEntity(bindingResult.getFieldErrors(), HttpStatus.BAD_REQUEST);

        TicketSet ticketSet = ticketSetForm.getTicketSet();
        event.addTicketSet(ticketSet);

        eventRepository.save(event);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(eventURI.ticketSetURL(userId, event.getId(), ticketSet.getId())));

        return new ResponseEntity(headers, HttpStatus.CREATED);
    }

    // TODO: we have to create some kind of notification resource if the price changes and there are basket items that references it.
    @RequestMapping(value = EventURI.TICKET_SET_URI, method = RequestMethod.PUT)
    public ResponseEntity updateTicketSet(@PathVariable UUID userId, @PathVariable long eventId,
                                          @PathVariable long ticketSetId, @RequestBody(required = false) @Valid TicketSetForm ticketSetForm,
                                          BindingResult bindingResult)
    {
        TicketSet ticketSet = ticketSetRepository.findById(ticketSetId);

        if (ticketSet == null)
            return NOT_FOUND;

        Event event = ticketSet.getEvent();
        // ticketSet doesn't belong to event
        if (!event.getId().equals(eventId))
            return NOT_FOUND;

        User user = event.getUser();
        // event doesn't belong to user
        if (!user.getId().equals(userId))
            return NOT_FOUND;

        if (!identityAuthorizer.authorize(user.getIdentity()))
            return FORBIDDEN;

        if (ticketSetForm == null)
            throw new HttpMessageNotReadableException("Request body must be provided");

        if (bindingResult.hasFieldErrors())
            return new ResponseEntity(bindingResult.getFieldErrors(), HttpStatus.BAD_REQUEST);

        //ticketSetRepository.save(ticketSet.merge(ticketSetForm));

        ticketSetRepository.saveOrUpdate(ticketSetForm.getTicketSet(ticketSet));

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    // TODO: Cannot delete the ticket set if some basket items still reference it.
    @RequestMapping(value = EventURI.TICKET_SET_URI, method = RequestMethod.DELETE)
    public ResponseEntity deleteTicketSet(@PathVariable UUID userId, @PathVariable long eventId, @PathVariable long ticketSetId)
    {
        TicketSet ticketSet = ticketSetRepository.findById(ticketSetId);

        if (ticketSet == null)
            return NOT_FOUND;

        Event event = ticketSet.getEvent();

        // TicketSet doesn't belong to event
        if (!event.getId().equals(eventId))
            return NOT_FOUND;

        User user = event.getUser();

        // Event doesn't belong to user
        if (!user.getId().equals(userId))
            return NOT_FOUND;

        if (!identityAuthorizer.authorize(user.getIdentity()))
            return FORBIDDEN;

        ticketSetRepository.delete(ticketSet);

        return new ResponseEntity(HttpStatus.OK);
    }


    /**
     * Parse comma separated list of expansion rules coming from request parameter
     *
     * @param rule
     * @return expansion rules in a String array
     */
    private String[] getExpansionRules(String rule)
    {
        return rule != null && !rule.isEmpty() ? rule.split(",") : new String[0];
    }

}
