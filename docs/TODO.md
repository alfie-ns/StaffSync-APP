# TODO

- [ ] talk about the singleton pattern learnt in week11
- [ ] thread-safe initialisation happens if the synchronized keyword is used
- [ ] get delete employee button working; pass the id to delete any employee regardless of where it is in the list

### Comp2000 API endpoints

- [X] GET /employees
- [X] GET /employees/get/int{id}
- [X] POST /employees/add
- [X] PUT /employees/edit/{id}
- [X] DELETE /employees/delete/{id}
- [X] GET /health

---

- [ ] log into user account
- [X] get 2000 last lecture checklist
- [ ] incorporate lecture about notifications in my app
- [X] LocalDataService
- [ ] user login; i need to workout why user's not logging in
  it could either be the database account entry or navigation?
- [ ] ensure the comments are capitialised at the top of the file or important functions

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

- [ ] Implement worker threads for API operations; implement endpoints into the app
  - [X] GET operations
  - [X] POST operations
  - [ ] PUT operations
  - [X] DELETE operations
- [ ] Error handling in parallel operations
- [X] Basic API connection
- [X] Test getAllEmployees function
- [X] Figure out how to test admin dashboard
- [X] Figure out how to integrate the API into the app
- [ ] Implement proper thread management

### Core Functionality

#### Admin Features

- [ ] Employee management:
  - [X] Add new employee user details
  - [X] View employee details for verification
  - [ ] Edit employee details
  - [X] Delete employee records
- [ ] Automatic 5% salary increment after one year
- [ ] View and manage holiday requests
- [ ] Push notification system
- [X] Implement collapse/minimize employee list functionality
- [ ] Enhance view of clicked employee in list
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

- [ ] View personal details
- [ ] Edit personal details
- [ ] Holiday request submission
- [ ] Notification preferences management
- [ ] 30-day holiday allowance management
- [ ] Remember getIncrementStatus for employee-side fragments

### Database Implementation

- [X] Design database schema
- [X] Create database
- [ ] Store user location
- [ ] Store user time
- [ ] Implement security measures
- [ ] Ensure scalability

## Design and Testing (40%)

### UI/UX Implementation

- [ ] Screen size adaptation
- [ ] Implement HCI principles:

  - [X] Clear navigation
  - [ ] Consistent layout
  - [ ] Error feedback
  - [ ] User control
- [ ] Warning message handling
- [ ] RecyclerViews implementation
- [ ] Responsive design testing
- [ ] Style resources implementation
- [ ] Extract dimension values to dimens.xml
- [ ] Improve main menu
- [ ] implement staffsync title underline
- [X] Move hardcoded strings to strings.xml
- [X] Put all strings in LoginFragment.xml into strings.xml
- [X] Implement onBackPressed for fragments

### Notification System

- [ ] Core notifications for:
  - [ ] Holiday requests and responses
  - [ ] Employee detail changes
  - [ ] Salary increments
  - [X] New employees
  - [ ] Employee departures
  - [ ] Employee birthdays
  - [ ] Employee anniversaries
  - [ ] Employee promotions/demotions
  - [ ] Anomaly detection alerts
- [ ] User preferences:
  - [ ] Toggle settings
  - [ ] Notification types
  - [ ] Delivery methods

### Testing & Evaluation

- [ ] Summative usability evaluation:
  - [ ] Minimum two users
  - [ ] Testing documentation
  - [ ] Demographics collection
  - [ ] Consent forms
- [ ] Technical testing:
  - [ ] Error handling
  - [ ] UI components
  - [ ] API integration
  - [ ] Thread safety
  - [ ] Notification system

## Documentation & Approach (20%)

### Documentation Requirements

- [ ] PDF Report (~2000 words):
  - [ ] Introduction
  - [ ] Background
  - [ ] LSEP considerations
  - [ ] Design documentation
  - [ ] Implementation details
  - [ ] Evaluation results
  - [ ] Summary
  - [ ] References
- [ ] Code Documentation:
  - [ ] Well-commented code
  - [ ] Use Java-styled commenting with stars
  - [X] Add more comments to code
  - [X] Refactor and rename files

### Video Documentation

- [ ] 3-5 minute video demonstration:
  - [ ] Design pattern explanations
  - [ ] Code walkthrough
  - [ ] Live data demonstration
  - [ ] Running application showcase

### Version Control

- [ ] Regular GitHub commits:
  - [ ] Weekly minimum commits
  - [ ] Meaningful commit messages
  - [ ] Proper branching strategy
- [ ] README documentation:
  - [ ] Setup instructions
  - [ ] Third-party credits
  - [ ] Feature documentation
