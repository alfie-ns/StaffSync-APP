# TODO

- [ ] CHANGE PASSWORD OPTION IN USER SETTINGS
- [ ] check is apiWorkerThread is null before queting getEmployeeById task
- [ ] ensure current leave days balance is defined and always shown
- [ ] talk about the singleton pattern learnt in week11
- [ ] highly secure password to be fetched and pasted in one click
- [ ] thread-safe initialisation happens if the synchronized keyword is used
- [X] get delete employee button working; pass the id to delete any employee regardless of where it is in the list
- [X] reinstate bare notification system
- [ ] implement the notification system i.e. holiday requests, employee detail changes, salary increments, new employees, employee departures, employee birthdays, employee anniversaries, employee promotions/demotions, anomaly detection alerts
- [ ] complete notification preferences system
- [ ] thread-safe operationgs for database operations
- [ ] login employee account that can interact with the API
- [ ] clear search text in the rright side of recyclerview search bar
- [ ] hav functionality that checks for the first time the newly created employee log in, it should ask them to change their password, their should also be a way to change the password in profilefragment
- [ ] link dark mode state to employee acounts
- [ ] dark mode in admin fragment
- [ ] get employee fragments working with comp2000 API
- [ ] ensure to make system to not allow same emails when creating employee accountt

* I subsequently had to make getEmployeeById static because the method is
* called directly on the class name (ApiDataService.getEmployeeById) rather than
* on an instance of the class (apiService.getEmployeeById) this is because in
* regards to Holiday requests, we need to access employee data without instantiating
* the ApiDataService class each time we validate or process a request

- clarify fully qualified employeeViewModel?
- 

- [ ] changw password on first loggin
- [ ] worket threads

### comp2000 API endpoints

- [X] GET /employees
- [X] GET /employees/get/int{id}
- [X] POST /employees/add
- [X] PUT /employees/edit/{id}
- [X] DELETE /employees/delete/{id}
- [X] GET /health

---

### Design

- [X] dark mode
- [X] dark mode 2; better dark-mode colours; implement dark mode respective current toggle state for dark mode for respective user
- [X] implement HCI principles:
  - [X] clear navigation
  - [X] consistent layout
  - [ ] error feedback
  - [X] user control
- [ ] warning message handling
- [X] RecyclerViews implementation in user fragments

### User

- [X] log into user account
- [X] bottom nav bar
- [X] fix logout button in settomgs fragment
- [X] get full user features working

---

- [X] get 2000 last lecture checklist
- [ ] incorporate lecture about notifications in my app
- [X] LocalDataService
- [X] user login; i need to workout why user's not logging in
  it could either be the database account entry or navigation?
- [X] ensure the comments are capitialised at the top of the file or important functions

## Core Technical Implementation (40%)

### Architecture & Design Patterns

- [ ] Implement non-monolithic architecture
- [ ] Create worker threads for API connections
- [X] Implement Singleton pattern in LocationTracker
- [X] Implement Observer pattern in StaffDataService
- [ ] Document pattern choices and benefits
- [ ] Demonstrate layer interactions
- [X] Implement parallelism in the login page (location tracking and error handling)
- [ ] Define Communication Interface protocols
- [ ] Establish serialization and marshaling procedures
- [ ] Handle data format translation across systems

### API Integration & Threading

- [ ] Implement worker threads implement endpoints into the app
  - [X] GET operations
  - [X] POST operations
  - [X] PUT operations
  - [X] DELETE operations
  - [X] HEALTH operations
- [ ] Error handling in parallel operations
- [X] Basic API connection
- [X] Test getAllEmployees function
- [X] Figure out how to test admin dashboard
- [X] Figure out how to integrate the API into the app
- [ ] Implement proper thread management

### Core Functionality

#### Admin Features

- [X] Employee management:
  - [X] Add new employee user details
  - [X] View employee details for verification
  - [X] Edit employee details
  - [X] Delete employee records
- [X] Automatic 5% salary increment after one year
- [X] View and manage holiday requests
- [X] Push notification system
- [X] Implement collapse/minimize employee list functionality
- [X] Enhance view of clicked employee in list
- [X] Search employee functionality
- [X] Improve search employee functionality filter function

#### Authentication & Security

- [X] Login security implementation
- [X] Admin login functionality
- [X] User login functionality
- [ ] Implement forgot password feature
- [ ] Location tracking
- [ ] Device recognition
- [ ] Implement anomaly detection:
  - [ ] Detect unusual time and location
  - [ ] Detect unrecognized device
  - [ ] Develop anomaly detection algorithm
  - [ ] Implement 2FA verification for anomalies

#### Employee Features

- [X] View personal details
- [X] Edit personal details
- [X] Holiday request submission
- [X] Notification preferences management
- [X] 30-day holiday allowance management
- [X] Remember getIncrementStatus for employee-side fragments

### Database Implementation

- [X] Design database schema
- [X] Create database
- [X] Implement SQLite database
- [X] Store user login details
- [X] Store user details
- [ ] Store user location
- [ ] Store user time
- [ ] Implement security measures
- [ ] Ensure scalability

## Design and Testing (40%)

### UI/UX Implementation

- [ ] Screen size adaptation
- [X] Implement HCI principles:

  - [X] Clear navigation
  - [X] Consistent layout
  - [ ] Error feedback
  - [X] User control
- [X] RecyclerViews implementation
- [X] Improve main menu
- [X] Move hardcoded strings to strings.xml
- [X] Put all strings in LoginFragment.xml into strings.xml
- [X] Implement onBackPressed for fragments

### Notification System

- [ ] Core notifications for:
  - [X] Holiday requests and responses
  - [ ] Salary increments
  - [X] New employees
  - [ ] Employee departures
  - [ ] Anomaly detection alerts
- [ ] User preferences:
  - [ ] Toggle settings
  - [ ] Delivery methods

### Testing & Evaluation

- [X] Summative usability evaluation:

  - [X] Minimum two users
  - [X] Demographics collection
  - [X] Consent forms
- [X] Technical testing:

  - [X] Error handling
  - [X] UI components
  - [X] API integration
  - [X] Thread safety
  - [X] Notification system

## Documentation & Approach (20%)

### Documentation Requirements

- [X] PDF Report (~2000 words):
  - [X] Introduction
  - [X] Background
  - [X] LSEP considerations
  - [X] Design documentation
  - [X] Implementation details
  - [X] Evaluation results
  - [X] Summary
  - [X] References
- [X] Code Documentation:
  - [X] Well-commented code
  - [X] Use Java-styled commenting with stars
  - [X] Add more comments to code
  - [X] Refactor and rename files
