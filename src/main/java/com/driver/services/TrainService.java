package com.driver.services;

import com.driver.EntryDto.AddTrainEntryDto;
import com.driver.EntryDto.SeatAvailabilityEntryDto;
import com.driver.model.Passenger;
import com.driver.model.Station;
import com.driver.model.Ticket;
import com.driver.model.Train;
import com.driver.repository.TrainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;

@Service
public class TrainService {

    @Autowired
    TrainRepository trainRepository;

    public Integer addTrain(AddTrainEntryDto trainEntryDto){
        trainEntryDto.setDepartureTime(LocalTime.now());
        //Add the train to the trainRepository
        Train train = new Train();
        train.setNoOfSeats(trainEntryDto.getNoOfSeats());
        train.setDepartureTime(trainEntryDto.getDepartureTime());
        //and route String logic to be taken from the Problem statement.
        StringBuilder route = new StringBuilder();
        List<Station> trainRoute = trainEntryDto.getStationRoute();

        for (Station s: trainRoute) {
            route.append(s);
            route.append(",");
        }
        route.deleteCharAt(route.length()-1);

        train.setRoute(route.toString());

        //Save the train and return the trainId that is generated from the database.
        Train savedTrain = trainRepository.save(train);
        //Avoid using the lombok library
        return savedTrain.getTrainId();
    }

    public Integer calculateAvailableSeats(SeatAvailabilityEntryDto seatAvailabilityEntryDto){

        //Calculate the total seats available
        //Suppose the route is A B C D
        //And there are 2 seats available in total in the train
        //and 2 tickets are booked from A to C and B to D.
        //The seat is available only between A to C and A to B. If a seat is empty between 2 station it will be counted to our final ans
        //even if that seat is booked post the destStation or before the boardingStation
        //Inshort : a train has totalNo of seats and there are tickets from and to different locations
        //We need to find out the available seats between the given 2 stations.

        Train train = trainRepository.findById(seatAvailabilityEntryDto.getTrainId()).get();
        String [] stations = train.getRoute().split(",");
        ArrayList<String> stationsList  = new ArrayList<>(Arrays.asList(stations));

        int totalSeatsInTrain = train.getNoOfSeats();
        int availableSeats = 0;
        Station fromStation = seatAvailabilityEntryDto.getFromStation();
        Station toStation = seatAvailabilityEntryDto.getToStation();
        String destinationStation = stationsList.get(stationsList.size()-1);

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

        return availableSeats;
    }

    public Integer calculatePeopleBoardingAtAStation(Integer trainId,Station station) throws Exception{

        //We need to find out the number of people who will be boarding a train from a particular station
        //if the trainId is not passing through that station
        //throw new Exception("Train is not passing from this station");
        //  in a happy case we need to find out the number of such people.
        Train train = trainRepository.findById(trainId).get();
        String [] stations = train.getRoute().split(",");
        ArrayList<String> stationsList  = new ArrayList<>(Arrays.asList(stations));
        if (!stationsList.contains(station.toString())) {
            throw new Exception("Train is not passing from this station");
        }

        List<Ticket> ticketList = train.getBookedTickets();

        int countOfPassengersBoarding = 0;

        for (Ticket ticket: ticketList) {
            if (ticket.getFromStation().equals(station)) {
                countOfPassengersBoarding+= ticket.getPassengersList().size();
            }
        }

        return countOfPassengersBoarding;
    }

    public Integer calculateOldestPersonTravelling(Integer trainId){

        //Throughout the journey of the train between any 2 stations
        //We need to find out the age of the oldest person that is travelling the train
        //If there are no people travelling in that train you can return 0
        Train train = trainRepository.findById(trainId).get();
        List<Ticket> ticketList = train.getBookedTickets();
        int ageOfOldestPerson = 0;
        for (Ticket t: ticketList) {
            for (Passenger p: t.getPassengersList()) {
                ageOfOldestPerson = Math.max(ageOfOldestPerson,p.getAge());
            }
        }
        return ageOfOldestPerson;
    }

    public List<Integer> trainsBetweenAGivenTime(Station station, LocalTime startTime, LocalTime endTime){

        //When you are at a particular station you need to find out the number of trains that will pass through a given station
        //between a particular time frame both start time and end time included.
        //You can assume that the date change doesn't need to be done ie the travel will certainly happen with the same date (More details
        //in problem statement)
        //You can also assume the seconds and milli seconds value will be 0 in a LocalTime format.
        List<Train> allTrains = trainRepository.findAll();
        List<Integer> listOfTrains = new ArrayList<>();

        for (Train train: allTrains) {
            LocalTime depTime = train.getDepartureTime();
            String [] stations = train.getRoute().split(",");
            ArrayList<String> stationsAsArrayList = new ArrayList<>(Arrays.asList(stations));
            if (stationsAsArrayList.contains(station.toString())) {
                LocalTime stationTime = depTime.plusHours(stationsAsArrayList.indexOf(station.toString()));
                if ((stationTime.isAfter(startTime) && stationTime.isBefore(endTime)) || stationTime.equals(startTime) || stationTime.equals(endTime)) {
                    listOfTrains.add(train.getTrainId());
                }
            }
        }
        return listOfTrains;
    }

}
