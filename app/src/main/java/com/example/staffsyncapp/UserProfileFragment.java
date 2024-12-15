package com.example.staffsyncapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.staffsyncapp.databinding.UserProfileFragmentBinding;
import com.example.staffsyncapp.models.Employee;
import com.example.staffsyncapp.utils.NavigationManager;

import java.util.List;


/** TODO
* [ ] implement functionality to edit own details
* [ ] test that the details change by looking on adminDashboard fragment
**/

public class UserProfileFragment extends Fragment {
    private UserProfileFragmentBinding binding;
    private ApiDataService apiService;
    private Employee currentEmployee;
    NavigationManager navigationManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = UserProfileFragmentBinding.inflate(inflater, container, false);
        apiService = new ApiDataService(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navigationManager = new NavigationManager(this, binding.bottomNavigation);
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_profile);

        setupValidation();
        loadEmployeeData();
        setupClickListeners();
    }

    private void setupValidation() {
        // email validation
        binding.editEmail.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmail();
            }

            public void afterTextChanged(Editable s) {}
        });

        // confirm email validation
        binding.confirmEditEmail.addTextChangedListener(new TextWatcher() {

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}


            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmailMatch();
            }


            public void afterTextChanged(Editable s) {}
        });
    }

    private boolean validateEmail() { // validate email format
        String email = binding.editEmail.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() && !email.isEmpty()) {
            binding.emailFormatError.setVisibility(View.VISIBLE);
            return false;
        }
        binding.emailFormatError.setVisibility(View.GONE);
        return true;
    }

    private boolean validateEmailMatch() { // validate email match
        String email = binding.editEmail.getText().toString().trim();
        String confirmEmail = binding.confirmEditEmail.getText().toString().trim();
        if (!email.equals(confirmEmail) && !confirmEmail.isEmpty()) {
            binding.emailMatchError.setVisibility(View.VISIBLE);
            return false;
        }
        binding.emailMatchError.setVisibility(View.GONE);
        return true;
    }

    private void loadEmployeeData() {
        // [ ] TODO: get employee ID from shared preferences
        int employeeId = 967; // [ ] TODO: implement actual logging into the respective employee/id

        apiService.getEmployeeById(employeeId, new ApiDataService.EmployeeFetchListener() {
            public void onEmployeesFetched(List<Employee> employees) {
                if (employees != null && !employees.isEmpty()) {
                    currentEmployee = employees.get(0);
                    updateUIWithEmployeeData(currentEmployee);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIWithEmployeeData(Employee employee) { // update UI with employee data
        binding.employeeId.setText("EMP" + employee.getId());
        binding.currentName.setText(employee.getFirstname() + " " + employee.getLastname());
        binding.currentEmail.setText(employee.getEmail());
        binding.currentSalary.setText(String.format("£%.2f", employee.getSalary()));
    }

    private void setupClickListeners() {
        binding.saveProfile.setOnClickListener(v -> {
            if (validateInput()) {
                updateEmployeeDetails();
            }
        });

        binding.backArrow.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });
    }

    private boolean validateInput() { // validate user input
        String newName = binding.editName.getText().toString().trim();
        if (newName.isEmpty()) {
            binding.nameFormatError.setVisibility(View.VISIBLE);
            return false;
        }
        binding.nameFormatError.setVisibility(View.GONE);

        return validateEmail() && validateEmailMatch();
    }

    private void updateEmployeeDetails() { // update employee details with JSON
        String newName = binding.editName.getText().toString().trim();
        String newEmail = binding.editEmail.getText().toString().trim();

        String[] names = newName.split(" ", 2);
        String firstName = names[0];
        String lastName = names.length > 1 ? names[1] : "";

        apiService.updateEmployee(
                currentEmployee.getId(),
                firstName,
                lastName,
                newEmail,
                currentEmployee.getDepartment(),
                currentEmployee.getSalary(),
                currentEmployee.getJoiningdate(),
                new ApiDataService.EmployeeUpdateListener() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigateUp();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
