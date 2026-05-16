# TravelHub - Travel Website

GitHub Repository: [https://github.com/vineet-saini/Travel_hub](https://github.com/vineet-saini/Travel_hub)

TravelHub is a full-stack travel web application built with **Spring Boot**, **Thymeleaf**, **Bootstrap**, and **Java**.  
It allows users to browse countries and places, view travel packages, book trips, manage bookings, and optimize itineraries using AI. 

---

## Table of Contents
1. [Features](#features)
2. [Prerequisites](#prerequisites)
3. [Setup & Installation](#setup--installation)
4. [Running the Project](#running-the-project)
5. [Project Structure](#project-structure)
6. [Usage](#usage)
7. [Future Enhancements](#future-enhancements)

---

## Features

- Home page with travel hero banner and destination cards
- Country pages with top places (India, UK, USA, Canada)
- Place page with travel packages and "Book Now" flow
- Traveller details form and booking confirmation page
- Payment page
- My Bookings — view and manage your bookings
- User Login, Signup, and Profile pages
- AI Trip Optimizer — enter destinations and get a day-wise optimized itinerary powered by Groq AI
- Contact page with popup confirmation
- Responsive design with Bootstrap

---

## Prerequisites

- **Java JDK 17** or later
- **Maven 3.8+**
- **MySQL Server**
- **Groq API Key** (free) — for AI Trip Optimizer → [console.groq.com](https://console.groq.com)
- **IDE**: IntelliJ, Eclipse, or VS Code

---

## Setup & Installation

1. Clone the repository:

```bash
git clone https://github.com/vineet-saini/Travel_hub.git
cd Travel_hub
```

2. Import the project in your IDE as a Maven project.

3. Configure the database — open `src/main/resources/application.properties` and update:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/traveldb?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

4. Set your Groq API key — create a `.env` file in the project root (see `.env.example`):

```
GROQ_API_KEY=your_groq_api_key_here
```

5. Make sure `src/main/resources/static/images/` contains the required travel images.

---

## Running the Project

```bash
mvn clean install
mvn spring-boot:run
```

Open browser: [http://localhost:8080/home](http://localhost:8080/home)

---

## Project Structure

```
TravelHub/
├── src/
│   ├── main/
│   │   ├── java/com/travel/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   └── TravelWebsiteApplication.java
│   │   └── resources/
│   │       ├── packages/              # Travel package text files
│   │       │   ├── Agra-3Day.txt
│   │       │   ├── Kashmir-3Day.txt
│   │       │   ├── Shimla-4Day.txt
│   │       │   ├── Mumbai-2Day.txt
│   │       │   ├── London-3Day.txt
│   │       │   ├── NewYork-3Day.txt
│   │       │   └── Toronto-3Day.txt
│   │       ├── static/images/         # Banner & place images
│   │       ├── templates/             # Thymeleaf HTML pages
│   │       │   ├── base.html
│   │       │   ├── home.html
│   │       │   ├── country.html
│   │       │   ├── place.html
│   │       │   ├── traveller-details.html
│   │       │   ├── booking.html
│   │       │   ├── booking-detail.html
│   │       │   ├── booking-success.html
│   │       │   ├── my-bookings.html
│   │       │   ├── payment.html
│   │       │   ├── trip-optimizer.html
│   │       │   ├── login.html
│   │       │   ├── signup.html
│   │       │   ├── profile.html
│   │       │   ├── about.html
│   │       │   └── contact.html
│   │       └── application.properties
├── .env.example
├── pom.xml
└── README.md
```

---

## Usage

1. Browse **Home** → select a country → choose a place
2. Click **Book Now** → fill traveller details → complete payment → see booking confirmation
3. Go to **My Bookings** to view all your bookings
4. Use **AI Trip Optimizer** → enter destinations, dates, budget & style → get a day-wise itinerary
5. Click **Contact** → submit a message → see popup confirmation

---

## Future Enhancements

- Email notifications for booking confirmation
- Real payment gateway integration
- Admin dashboard for managing packages and bookings
