package com.driver.services;


import com.driver.EntryDto.BookTicketEntryDto;
import com.driver.EntryDto.SeatAvailabilityEntryDto;
import com.driver.model.Passenger;
import com.driver.model.Ticket;
import com.driver.model.Train;
import com.driver.repository.PassengerRepository;
import com.driver.repository.TicketRepository;
import com.driver.repository.TrainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class TicketService {

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    TrainRepository trainRepository;

    @Autowired
    PassengerRepository passengerRepository;


    public Integer bookTicket(BookTicketEntryDto bookTicketEntryDto)throws Exception{

        //Check for validity
        Optional<Train> trainOptional = trainRepository.findById(bookTicketEntryDto.getTrainId());
        if (!trainOptional.isPresent()) {
            throw new Exception("Train Id not valid");
        }

        Train train = trainOptional.get();
        //Incase the train doesn't pass through the requested stations
        //throw new Exception("Invalid stations");
        String [] stations = train.getRoute().split(",");
        ArrayList<String> stationsAsArrayList = new ArrayList<>(Arrays.asList(stations));
        if (!stationsAsArrayList.contains(bookTicketEntryDto.getFromStation().toString())
                || !stationsAsArrayList.contains(bookTicketEntryDto.getToStation().toString())) {
            throw new Exception("Invalid stations");
        }

        //Use bookedTickets List from the TrainRepository to get bookings done against that train
        List<Ticket> bookedTicketsOfTrain = new ArrayList<>();
        if (train.getBookedTickets() != null) {
            bookedTicketsOfTrain = train.getBookedTickets();
        }
        // Incase the there are insufficient tickets
        // throw new Exception("Less tickets are available");


        //otherwise book the ticket, calculate the price and other details
        List<Integer> passengerIdList = bookTicketEntryDto.getPassengerIds();
        List<Passenger> passengerList = new ArrayList<>();
        for (Integer id: passengerIdList) {
            Optional<Passenger> passengerOptional = passengerRepository.findById(id);
            if (passengerOptional.isPresent()) {
                Passenger passenger = passengerOptional.get();
                passengerList.add(passenger);
            }
        }

        Ticket ticket = new Ticket();
        ticket.setFromStation(bookTicketEntryDto.getFromStation());
        ticket.setToStation(bookTicketEntryDto.getToStation());
        ticket.setTrain(train);
        ticket.setPassengersList(passengerList);
        //Fare System : Check problem statement
        int intermediateStations = stationsAsArrayList.indexOf(bookTicketEntryDto.getToStation().toString())
                - stationsAsArrayList.indexOf(bookTicketEntryDto.getFromStation().toString());

        int totalFare = intermediateStations*300;

        ticket.setTotalFare(totalFare);
        //Save the information in corresponding DB Tables
        Ticket savedTicket = ticketRepository.save(ticket);

        for (Passenger p: passengerList) {
            List<Ticket> bookedTicketsOfPassenger = new ArrayList<>();
            if (p.getBookedTickets() != null) {
                bookedTicketsOfPassenger = p.getBookedTickets();
                bookedTicketsOfPassenger.add(savedTicket);
                passengerRepository.save(p);
            }
        }

        bookedTicketsOfTrain.add(savedTicket);
        trainRepository.save(train);


        //Save the bookedTickets in the train Object
        //Also in the passenger Entity change the attribute bookedTickets by using the attribute bookingPersonId.
       //And the end return the ticketId that has come from db

       return savedTicket.getTicketId();

    }
}
