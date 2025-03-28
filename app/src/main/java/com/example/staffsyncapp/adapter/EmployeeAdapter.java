package com.example.staffsyncapp.adapter;

// Android libraries for logging and context usage testing
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

// RecyclerView libraries for displaying employee data
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.RecyclerView;

// Employee model class
import com.example.staffsyncapp.R;
import com.example.staffsyncapp.models.Employee;

// NumberFormat and Locale libraries for currency formatting
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;



public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {
    // EmployeeAdapter class for displaying employee data in a RecyclerView, dynamically; going to be used elsewhere in the future
    private static final String TAG = "EmployeeAdapter";
    private final List<Employee> employees;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.UK);
    private static final String ID_FORMAT = "(ID: %d)"; // defined static to be accessible app-wide via EmployeeAdapter.ID_FORMAT
    private List<Employee> employeesFull;

    private OnEmployeeDeleteListener deleteListener;
    private OnEmployeeUpdateListener updateListener;

    public EmployeeAdapter(List<Employee> employees) { // Employee list constructor
        // create new lists to avoid reference issues
        this.employees = new ArrayList<>(employees);
        this.employeesFull = new ArrayList<>(employees);
        Log.d(TAG, "EmployeeAdapter initialised with " + employees.size() + " employees");
    }

    public interface OnEmployeeDeleteListener {
        void onDeleteClicked(Employee employee);
    }

    public interface OnEmployeeUpdateListener {
        void onUpdateClicked(Employee employee);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.admin_employee_item_recycler, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) { // bind employee data to the view
        try {
            Employee employee = employees.get(position);
            String employeeName = employee.getName();

            holder.nameTextView.setText(employeeName.isEmpty() ? "N/A" : employeeName);
            holder.idTextView.setText(String.format(Locale.UK, ID_FORMAT, employee.getId())); // use UK locale for consistent ID formatting
            holder.emailTextView.setText(employee.getEmail() != null ? employee.getEmail() : "N/A");
            holder.departmentTextView.setText(employee.getDepartment() != null ? employee.getDepartment() : "N/A");
            holder.salaryTextView.setText(currencyFormatter.format(employee.getSalary()));

            holder.deleteIcon.setOnClickListener(v -> { // delete employee bin icon onClickListener
                if (deleteListener != null) {
                    new AlertDialog.Builder(holder.itemView.getContext())
                            .setTitle("Delete Employee")
                            .setMessage("Are you sure you want to delete " + employeeName + " " +
                                    String.format(Locale.UK, ID_FORMAT, employee.getId()) + "?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                deleteListener.onDeleteClicked(employee);
                            })
                            .setNegativeButton("No", null)
                            .show();
                }
            });

            holder.updateIcon.setOnClickListener(v -> {
                if (updateListener != null) {
                    updateListener.onUpdateClicked(employee);
                }
            });

    
        } catch (Exception e) {
            Log.e(TAG, "Error binding employee at position " + position, e);
            holder.nameTextView.setText("Error loading employee");
            holder.emailTextView.setText("N/A");
            holder.departmentTextView.setText("N/A");
            holder.salaryTextView.setText("N/A");
        }
    }

    @Override
    public int getItemCount() {
        return employees.size();
    }

    public void setOnEmployeeDeleteListener(OnEmployeeDeleteListener listener) { // set delete listener
        this.deleteListener = listener;
    }

    public void setOnEmployeeUpdateListener(OnEmployeeUpdateListener listener) {
        this.updateListener = listener;
    }
    
    // Update adapter's list without changing total employee count
    public void updateDisplayList(List<Employee> displayedEmployees) {
        this.employees.clear();
        this.employees.addAll(displayedEmployees);
        notifyDataSetChanged(); // refresh the RecyclerView
    }


    /**
     * Filter the employee list based on the search text;
     * - if search text is empty, show full list
     * - case-insensitive search
     * - search through the backup list
     * - notify adapter that data has changed
     * @param text
     */
     
    public void filter(String text) {
        try { // TODO SearchByID [X]
            // clear current list but preserve the fullList
            employees.clear();

            // if search text is empty, show full list
            if (text == null || text.isEmpty()) {
                employees.addAll(employeesFull);
            } else {
                // case-insensitive search
                text = text.toLowerCase();

                // search through the backup list
                for (Employee employee : employeesFull) {
                    if (employee.getFirstname().toLowerCase().contains(text) ||
                            employee.getLastname().toLowerCase().contains(text) ||
                            employee.getEmail().toLowerCase().contains(text) ||
                            employee.getDepartment().toLowerCase().contains(text)) {
                        employees.add(employee);
                    }
                }
            }

            // notify adapter that data has changed
            notifyDataSetChanged();
            Log.d(TAG, "Filter applied with text: '" + text + "'. Results: " + employees.size());

        } catch (Exception e) { // restore the full list if exception
            Log.e(TAG, "Error filtering employees...", e);
            employees.clear();
            employees.addAll(employeesFull);
            notifyDataSetChanged();
        }
    }

    // Define ViewModel for employee data
    public static class EmployeeViewModel extends ViewModel {
        private MutableLiveData<Employee> employeeLiveData = new MutableLiveData<>();

        public LiveData<Employee> getEmployeeLiveData() {
            return employeeLiveData;
        }

        public void setEmployeeData(Employee employee) {
            employeeLiveData.setValue(employee);
        }
    }

    // Define ViewHolder for caching employee data views thus no repeated findViewById() calls
    public static class ViewHolder extends RecyclerView.ViewHolder { // ViewHolder for caching employee data views thus no repeated findViewById() calls
        final TextView nameTextView;
        final TextView idTextView;
        final TextView emailTextView;
        final TextView departmentTextView;
        final TextView salaryTextView;
        final ImageView deleteIcon;
        final ImageView updateIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            idTextView = itemView.findViewById(R.id.idTextView);
            emailTextView = itemView.findViewById(R.id.emailTextView);
            departmentTextView = itemView.findViewById(R.id.departmentTextView);
            salaryTextView = itemView.findViewById(R.id.salaryTextView);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
            updateIcon = itemView.findViewById(R.id.pencilIcon);
        }
    }
}