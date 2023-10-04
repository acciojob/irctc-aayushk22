package com.driver.services;


import com.driver.EntryDto.BookTicketEntryDto;
import com.driver.EntryDto.SeatAvailabilityEntryDto;
import com.driver.model.Passenger;
import com.driver.model.Station;
import com.driver.model.Ticket;
import com.driver.model.Train;
import com.driver.repository.PassengerRepository;
import com.driver.repository.TicketRepository;
import com.driver.repository.TrainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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
        ArrayList<String> stationsList = new ArrayList<>(Arrays.asList(stations));
        if (!stationsList.contains(bookTicketEntryDto.getFromStation().toString())
                || !stationsList.contains(bookTicketEntryDto.getToStation().toString())) {
            throw new Exception("Invalid stations");
        }

        //Use bookedTickets List from the TrainRepository to get bookings done against that train
        List<Ticket> bookedTicketsOfTrain = new ArrayList<>();
        if (train.getBookedTickets() != null) {
            bookedTicketsOfTrain = train.getBookedTickets();
        }
        // Incase the there are insufficient tickets
        // throw new Exception("Less tickets are available");
        int totalSeatsInTrain = train.getNoOfSeats();
        int availableSeats = 0;
        Station fromStation = bookTicketEntryDto.getFromStation();
        Station toStation = bookTicketEntryDto.getToStation();

        HashMap<String,Integer> freqOfPassengersAtStation = new HashMap<>();
        for (String s: stationsList) {
            freqOfPassengersAtStation.put(s,0);
        }
        List<Ticket> ticketList = train.getBookedTickets();

        for (Ticket t : ticketList) {
            boolean flag = false;
            for (String s : stationsList) {
                if (s.equals(t.getFromStation().toString())) flag = true;
                if (s.equals(t.getToStation().toString())) break;
                if (flag) freqOfPassengersAtStation.put(s, freqOfPassengersAtStation.get(s) + t.getPassengersList().size());
            }
        }

        boolean bool = false;
        for (String s: stationsList) {
            if (s.equals(fromStation.toString())) bool = true;
            if (s.equals(toStation.toString())) break;
            if (bool) availableSeats += totalSeatsInTrain - freqOfPassengersAtStation.get(s);
        }

        if (availableSeats < bookTicketEntryDto.getPassengerIds().size()) {
            throw new Exception("Less tickets are available");
        }

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
        int intermediateStations = stationsList.indexOf(bookTicketEntryDto.getToStation().toString())
                - stationsList.indexOf(bookTicketEntryDto.getFromStation().toString());

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
