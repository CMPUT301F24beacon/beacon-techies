package com.example.event_lottery;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminListFacilityAcitivity extends AppCompatActivity {
    private static final String TAG = "AdminListFacility";
    private FirebaseFirestore db;
    private List<AdminFacility> facilityList;
    private FacilityListAdapter adapter;

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the most recent data supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_list_facility);

        db = FirebaseFirestore.getInstance();
        facilityList = new ArrayList<>();
        ListView listView = findViewById(R.id.list_view);
        adapter = new FacilityListAdapter(this, facilityList);
        listView.setAdapter(adapter);

        // Back button functionality
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        fetchFacilities();
    }

    /**
     * Called when the activity becomes visible to the user.
     */
    @Override
    protected void onResume() {
        super.onResume();
        fetchFacilities();
    }

    /**
     * Fetches the list of facilities from Firebase Firestore and updates the adapter.
     */
    private void fetchFacilities() {
        db.collection("facilities")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    facilityList.clear();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        AdminFacility facility = document.toObject(AdminFacility.class);
                        if (facility != null) {
                            facility.setFacilityId(document.getId());
                            facilityList.add(facility);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching facilities", e);
                    Toast.makeText(this, "Error loading facilities", Toast.LENGTH_SHORT).show();
                });
    }
}
