# Employee Management API Documentation

The Employee Management API provides endpoints to manage employee information within the system. It allows you to create, retrieve, update, and delete employee records, as well as perform a health check to ensure the API is operational.

## Base URL

- **URL**: `http://10.224.41.11/comp2000`
- Connect to university network to access the url.

## Endpoints

### 1. Get All Employees

- **URL**: `/employees`
- **Method**: `GET`
- **Description**: Retrieve a list of all employees.
- **Response**:
  - **200 OK**: Returns a JSON array of employee objects.
  - **Sample Response**:
    ```json
    [
      {
        "id": 1,
        "firstname": "John",
        "lastname": "Doe",
        "email": "john.doe@example.com",
        "department": "HR",
        "salary": 50000,
        "joiningdate": "2023-01-15"
      },
      ...
    ]
    ```

### 2. Get Employee by ID

- **URL**: `/employees/get/<int:id>`
- **Method**: `GET`
- **Description**: Retrieve details of a specific employee by their ID.
- **Parameters**:
  - `id`: Integer. The ID of the employee to retrieve.
- **Response**:
  - **200 OK**: Returns a JSON object of the employee.
  - **404 Not Found**: Returns an error message if the employee is not found.
  - **Sample Response**:
    ```json
    {
      "id": 1,
      "firstname": "John",
      "lastname": "Doe",
      "email": "john.doe@example.com",
      "department": "HR",
      "salary": 50000,
      "joiningdate": "2023-01-15"
    }
    ```

### 3. Add a New Employee

- **URL**: `/employees/add`
- **Method**: `POST`
- **Description**: Add a new employee to the system.
- **Request Body** (JSON):
  - `firstname`: String. Required.
  - `lastname`: String. Required.
  - `email`: String. Required.
  - `department`: String. Required.
  - `salary`: Float. Required.
  - `joiningdate`: String (YYYY-MM-DD). Required.
- **Response**:
  - **201 Created**: Returns a success message upon creation.
  - **Sample Response**:
    ```json
    {
      "message": "Employee added successfully"
    }
    ```

### 4. Update an Employee's Details

- **URL**: `/employees/edit/<int:id>`
- **Method**: `PUT`
- **Description**: Update an existing employee’s information.
- **Parameters**:
  - `id`: Integer. The ID of the employee to update.
- **Request Body** (JSON):
  - `firstname`: String. Optional.
  - `lastname`: String. Optional.
  - `email`: String. Optional.
  - `department`: String. Optional.
  - `salary`: Float. Optional.
  - `joiningdate`: String (YYYY-MM-DD). Optional.
- **Response**:
  - **200 OK**: Returns a success message if the update is successful.
  - **404 Not Found**: Returns an error message if the employee is not found.
  - **Sample Response**:
    ```json
    {
      "message": "Employee updated successfully"
    }
    ```

### 5. Delete an Employee

- **URL**: `/employees/delete/<int:id>`
- **Method**: `DELETE`
- **Description**: Delete an employee by their ID.
- **Parameters**:
  - `id`: Integer. The ID of the employee to delete.
- **Response**:
  - **200 OK**: Returns a success message upon deletion.
  - **404 Not Found**: Returns an error message if the employee is not found.
  - **Sample Response**:
    ```json
    {
      "message": "Employee deleted successfully"
    }
    ```

### 6. Health Check

- **URL**: `/health`
- **Method**: `GET`
- **Description**: Check if the API is running.
- **Response**:
  - **200 OK**: Returns a status message indicating the API is operational.
  - **Sample Response**:
    ```json
    {
      "status": "API is working"
    }
    ```

## Error Responses

Common error responses for the API include:

- **404 Not Found**: Returned if the employee ID is not found for `GET`, `PUT`, and `DELETE` operations.
- **500 Internal Server Error**: Returned for unexpected errors during request processing.
