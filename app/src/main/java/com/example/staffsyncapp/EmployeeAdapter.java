package com.example.staffsyncapp;

// Android libraries for logging and context usage testing
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

// RecyclerView libraries for displaying employee data
import androidx.annotation.NonNull;
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
    // EmployeeAdapter class for displaying employee data in a RecyclerView, dynamically
    private static final String TAG = "EmployeeAdapter";
    private final List<Employee> employees;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.UK);
    private List<Employee> employeesFull;

    public EmployeeAdapter(List<Employee> employees) {
        this.employees = employees;
        this.employeesFull = new ArrayList<>(employees);
        Log.d(TAG, "EmployeeAdapter initialised with " + employees.size() + " employees");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) { // bind employee data to the view
        try {
            Employee employee = employees.get(position);
            String employeeName = employee.getName();

            holder.nameTextView.setText(employeeName.isEmpty() ? "N/A" : employeeName);
            holder.emailTextView.setText(employee.getEmail() != null ? employee.getEmail() : "N/A");
            holder.departmentTextView.setText(employee.getDepartment() != null ? employee.getDepartment() : "N/A");
            holder.salaryTextView.setText(currencyFormatter.format(employee.getSalary()));

            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(holder.itemView.getContext(),
                        "Clicked: " + employeeName,
                        Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) { // catch any exceptions; log and display error message
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

    public void filter(String text) { // filter employees based on search text
        try {
            employees.clear();
            if (text.isEmpty()) {
                employees.addAll(employeesFull);
            } else {
                text = text.toLowerCase(); // case-insensitive search
                for (Employee employee : employeesFull) {
                    if (employee.getFirstname().toLowerCase().contains(text) ||
                            employee.getLastname().toLowerCase().contains(text) ||
                            employee.getEmail().toLowerCase().contains(text) ||
                            employee.getDepartment().toLowerCase().contains(text)) {
                        employees.add(employee);
                    }
                }
            }
            notifyDataSetChanged(); // notify the adapter that the data has changed thus refreshing the RecyclerView
            Log.d(TAG, "Filter applied with text: " + text + ". Results: " + employees.size());
        } catch (Exception e) {
            Log.e(TAG, "Error filtering employees", e);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder { // ViewHolder for caching employee data views thus no repeated findViewById() calls
        final TextView nameTextView;
        final TextView emailTextView;
        final TextView departmentTextView;
        final TextView salaryTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            emailTextView = itemView.findViewById(R.id.emailTextView);
            departmentTextView = itemView.findViewById(R.id.departmentTextView);
            salaryTextView = itemView.findViewById(R.id.salaryTextView);
        }
    }
}