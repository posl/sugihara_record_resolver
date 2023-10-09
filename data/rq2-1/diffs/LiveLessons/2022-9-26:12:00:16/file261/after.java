package datamodels;

import java.time.LocalDateTime;

/**
 * Data structure that defines a request for a trip.
 */
public class TripRequest {
    /**
     * Date and time of the departure.
     */
    private LocalDateTime departureDateTime;

    /**
     * Date and time of the return departure.
     */
    private LocalDateTime returnDepartureDateTime;

    /**
     * Airport code for the departing airport.
     */
    private String departureAirport;

    /**
     * Airport code for the arriving airport.
     */
    private String arrivalAirport;

    /**
     * Requested currency.
     */
    private String currency;

    /**
     * Number of passengers.
     */
    private Integer passengers;

    /**
     * Default constructor.
     */
    public TripRequest() {
    }

    /**
     * Constructor initializes the fields.
     * @param departureDateTime Departure date
     * @param returnDepartureDateTime Return date
     * @param departureAirport Departure airport
     * @param arrivalAirport Arrival airport
     * @param currency Requested currency
     * @param passengers Number of passengers
     */
    public TripRequest(LocalDateTime departureDateTime,
                       LocalDateTime returnDepartureDateTime,
                       String departureAirport,
                       String arrivalAirport,
                       String currency,
                       int passengers) {
        this.departureDateTime = departureDateTime;
        this.returnDepartureDateTime = returnDepartureDateTime;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.currency = currency;
        this.passengers = passengers;
    }

    /**
     * @return Returns true if there's a match between this {@code
     * TripRequest} and the {@code tripRequest} param
     */
    @Override
    public boolean equals(Object tripRequest) {
        if (tripRequest.getClass() != this.getClass())
            return false;

        TripRequest t = (TripRequest) tripRequest;
        return this.departureDateTime.toLocalDate()
            .equals(t.departureDateTime.toLocalDate())
            && this.returnDepartureDateTime.toLocalDate()
            .equals(t.returnDepartureDateTime.toLocalDate())
            && this.departureAirport
            .equals(t.departureAirport)
            && this.arrivalAirport
            .equals(t.arrivalAirport);
    }

    /**
     * @return Returns true if there's a match between this
     * TripRequest and the {@code tripResponse} param
     */
    public boolean equals(Flight tripResponse) {
        return this.departureDateTime.toLocalDate()
            .equals(tripResponse.departureDateTime.toLocalDate())
            && this.returnDepartureDateTime.toLocalDate()
            .equals(tripResponse.returnDepartureDateTime.toLocalDate())
            && this.departureAirport
            .equals(tripResponse.departureAirport)
            && this.arrivalAirport
            .equals(tripResponse.arrivalAirport);
    }

    /**
     * @return A String representation of this {@code TripRequest}
     */
    @Override
    public String toString() {
        return departureDateTime
            + ", "
            + returnDepartureDateTime
            + ", "
            + departureAirport
            + ", "
            + arrivalAirport
            + ", "
            + currency
            + ", "
            + passengers;
    }

    /**
     * Gets the departure date and time.
     */
    public LocalDateTime getDepartureDateTime() {
        return departureDateTime;
    }

    /**
     * Sets the departure date and time.
     */
    public void setDepartureDateTime(LocalDateTime departureDate) {
        this.departureDateTime = departureDate;
    }

    /**
     * Gets the return departure date and time.
     */
    public LocalDateTime getReturnDepartureDateTime() {
        return returnDepartureDateTime;
    }

    /**
     * Sets the return departure date and time.
     */
    public void setReturnDateTime(LocalDateTime returnDepartureDateTime) {
        this.returnDepartureDateTime = returnDepartureDateTime;
    }

    /**
     * Gets the departure airport code.
     */
    public String getDepartureAirport() {
        return departureAirport;
    }

    /**
     * Sets the departure airport code.
     */
    public void setDepartureAirport(String departureAirport) {
        this.departureAirport = departureAirport;
    }

    /**
     * Gets the arrival airport code.
     */
    public String getArrivalAirport() {
        return arrivalAirport;
    }

    /**
     * Sets the arrival airport code.
     */
    public void setArrivalAirport(String arrivalAirport) {
        this.arrivalAirport = arrivalAirport;
    }

    /**
     * Gets the requested currency.
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Sets the requested currency.
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Gets the number of passengers.
     */
    public int getPassengers() {
        return passengers;
    }

    /**
     * Sets the number of passengers.
     */
    public void setPassengers(int passengers) {
        this.passengers = passengers;
    }

    /**
     * Factory method that creates a new {@code Trip} object.
     *
     * @param departureDateTime Departure date
     * @param returnDepartureDateTime Return date
     * @param departureAirport Departure airport
     * @param arrivalAirport Arrival airport
     * @param currency Requested currency
     * @param passengers Number of passengers
     * @return An initialized {@code TripRequest}
     */
    public static TripRequest valueOf(LocalDateTime departureDateTime,
                                      LocalDateTime returnDepartureDateTime,
                                      String departureAirport,
                                      String arrivalAirport,
                                      String currency,
                                      int passengers) {
        return new TripRequest(departureDateTime,
                               returnDepartureDateTime,
                               departureAirport,
                               arrivalAirport,
                               currency,
                               passengers);
    }
}
