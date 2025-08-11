package com.llavore.hereoattendance;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.model.User;
import com.llavore.hereoattendance.utils.ProductDetails;

import java.util.ArrayList;
import java.util.List;

public class DashboardLessonActivity extends AppCompatActivity {

    private RecyclerView productRecyclerView;
    private ProductAdapter productAdapter;
    private List<ProductDetails> productList;
    private DatabaseReference databaseReference;

    private static final String TAG = "DashboardLesson";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard_lesson);
        

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setView(R.layout.loaderlayout);
        AlertDialog dialog = builder.create();
        dialog.show();

        initViews();
        setupRecyclerView();
        loadProductsFromDatabase(dialog);
    }
    




    private void initViews() {
        productRecyclerView = findViewById(R.id.productList);
        databaseReference = FirebaseDatabase.getInstance("https://hereo-attendanceproducts-bdcb4.firebaseio.com").getReference();
        productList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(this, productList);
        // Use 2 columns for better display of 3 products
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        productRecyclerView.setLayoutManager(layoutManager);
        productRecyclerView.setAdapter(productAdapter);
        
        // Add spacing between items (16dp)
        int spacing = 16;
        productRecyclerView.addItemDecoration(new GridSpacingItemDecoration(2, spacing, true));
    }

    private void loadProductsFromDatabase(AlertDialog dialog) {
        DatabaseReference productsRef = databaseReference.child("products");

        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                productList.clear();

                for (DataSnapshot productSnapshot : dataSnapshot.getChildren()) {
                    try {
                        ProductDetails product = productSnapshot.getValue(ProductDetails.class);
                        if (product != null) {
                            productList.add(product);
                            dialog.dismiss();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing product: " + productSnapshot.getKey(), e);
                    }
                }

                productAdapter.notifyDataSetChanged();
                Log.d(TAG, "Loaded " + productList.size() + " products");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                dialog.dismiss();
                Log.e(TAG, "Error loading products", databaseError.toException());
                Toast.makeText(DashboardLessonActivity.this, "Failed to load products", Toast.LENGTH_SHORT).show();
            }
        });
    }
}