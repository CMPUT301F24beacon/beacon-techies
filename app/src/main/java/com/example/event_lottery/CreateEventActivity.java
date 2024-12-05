package com.example.event_lottery;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Activity for creating new events.
 * Allows users to input event details, generate a QR code, and optionally upload an event image.
 * Handles saving event data to Firestore and images to Firebase Storage.
 */

public class CreateEventActivity extends AppCompatActivity {

    private static final String TAG = "CreateEventActivity";
    private static final int IMAGE_UPLOAD_REQUEST_CODE = 1; // Request code for image

    private Button btnCreateEvent, btnCancel;
    private FirebaseFirestore db;
    private ImageView qrCodeImageView, imgEventImage, btnBack, editImageBtn;
    private EditText etEventDateTime;
    private Calendar calendar;
    private Switch switchGeolocation;
    private String eventId; // Use event name as the event ID
    private String imageUrl; // Store the image URL

    /**
     * Initializes the activity and sets up event creation UI components.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied. Otherwise, it is null.
     */


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        db = FirebaseFirestore.getInstance();

        btnCreateEvent = findViewById(R.id.btn_create_and_generate_qr);

        btnCancel = findViewById(R.id.btn_cancel);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        switchGeolocation = findViewById(R.id.switch_geolocation);
        imgEventImage = findViewById(R.id.img_event_image);
        editImageBtn = findViewById(R.id.edit_image);

        etEventDateTime = findViewById(R.id.et_event_datetime);
        calendar = Calendar.getInstance();

        etEventDateTime.setOnClickListener(v -> showDatePicker());

        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());



        btnCreateEvent.setOnClickListener(v -> {
            String eventName = ((EditText) findViewById(R.id.et_event_name)).getText().toString().trim();
            String eventDateTime = etEventDateTime.getText().toString().trim();
            String capacity = ((EditText) findViewById(R.id.et_capacity)).getText().toString().trim();
            String price = ((EditText) findViewById(R.id.et_price)).getText().toString().trim();
            String description = ((EditText) findViewById(R.id.et_event_description)).getText().toString().trim();

            if (eventName.isEmpty() || eventDateTime.isEmpty() || capacity.isEmpty() || price.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date;
            try {
                date = dateFormat.parse(eventDateTime);
            } catch (ParseException e) {
                Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean geolocationEnabled = switchGeolocation.isChecked();

            // Save event without image URL initially
            saveEventToFirestore(eventName, date, capacity, price, description, geolocationEnabled, imageUrl);

            String qrContent = "myapp://event/" + eventName;


            Bitmap qrCodeBitmap = generateQRCode(qrContent);
            if (qrCodeBitmap != null) {
                qrCodeImageView.setImageBitmap(qrCodeBitmap);



                Map<String, Object> qrData = new HashMap<>();
                qrData.put("qrContent", qrContent); // Save the QR content
                qrData.put("qrhash", generateHashFromBitmap(qrCodeBitmap)); // Save the hash

                db.collection("events").document(eventName)
                        .set(qrData, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Event and QR Code stored successfully", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to store QR Code data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

            } else {
                Toast.makeText(this, "Failed to generate QR Code", Toast.LENGTH_SHORT).show();
            }
        });


        editImageBtn.setOnClickListener(v -> {
            Intent intent = new Intent(CreateEventActivity.this, EventImageUploadActivity.class);
            startActivityForResult(intent, IMAGE_UPLOAD_REQUEST_CODE); // Open image upload activity
        });

        btnCancel.setOnClickListener(v -> finish()); // Close the activity
    }

    // Setter for Firestore to allow testing with mock instances
    public void setFirestore(FirebaseFirestore firestore) {
        this.db = firestore;
    }

    /**
     * Displays a date picker dialog for the user to select an event date.
     */

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    showTimePicker();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    /**
     * Displays a time picker dialog for the user to select an event time.
     */

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    etEventDateTime.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    /**
     * Saves event data to Firestore.
     *
     * @param eventName           The name of the event.
     * @param eventDateTime       The date and time of the event.
     * @param capacity            The capacity of the event.
     * @param price               The price of the event.
     * @param description         A brief description of the event.
     * @param geolocationEnabled  Whether geolocation is enabled for the event.
     * @param imageUrl         The image path or URL of the event.
     */

    public void saveEventToFirestore(String eventName, Date eventDateTime, String capacity, String price, String description, boolean geolocationEnabled, String imageUrl) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventName", eventName);
        eventData.put("eventDateTime", eventDateTime);
        eventData.put("capacity", capacity);
        eventData.put("price", price);
        eventData.put("description", description);
        eventData.put("geolocationEnabled", geolocationEnabled);
        eventData.put("imageUrl", imageUrl);


        db.collection("events").document(eventName)
                .set(eventData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event created successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_UPLOAD_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra("imageUrl")) {
                imageUrl = data.getStringExtra("imageUrl");
                loadImageFromUrl(imageUrl);
            } else {
                Toast.makeText(this, "Failed to retrieve uploaded image URL", Toast.LENGTH_SHORT).show();
            }
        }
    }



    /**
     * Saves the selected image to Firestore Storage and retrieves its URL.
     * <p>
     * Compresses the image into JPEG format, uploads it to Firestore Storage,
     * and updates the Firestore database with the image URL.
     * </p>
     *
     * @param bitmap The image selected by the user
     */

    void saveImageToFirestore(Bitmap bitmap) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("event_images/" + System.currentTimeMillis() + ".jpg");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        storageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            // Save the image URL in Firestore
                            saveImageUrlToFirestore(uri.toString());
                            loadImageFromUrl(uri.toString());
                            Toast.makeText(CreateEventActivity.this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateEventActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    /**
     * Loads an image from a URL into the ImageView using Glide.
     * <p>
     * Displays a placeholder image while loading, and an error image if loading fails.
     * </p>
     *
     * @param url The URL of the image to load
     */

    private void loadImageFromUrl(String url) {
        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_image_placeholder) // Add a default placeholder
                .error(R.drawable.ic_error) // Add an error placeholder
                .into(imgEventImage);
    }

    /**
     * Saves the image URL to Firestore for the associated event.
     *
     * @param imageUrl The URL of the uploaded image
     */

    private void saveImageUrlToFirestore(String imageUrl) {
        DocumentReference docRef = db.collection("events").document(eventId);
        docRef.update("imageUrl", imageUrl)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Image updated successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update image URL", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }


    private void updateImageView(String imageUrl) {
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder) // Add a default placeholder
                .into(imgEventImage);
    }




    /**
     * Generates a QR code for the specified text.
     *
     * @param text The content to encode in the QR code.
     * @return A Bitmap representation of the QR code, or null if an error occurs.
     */

    public Bitmap generateQRCode(String text) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            int width = 300;
            int height = 300;
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String generateHashFromBitmap(Bitmap bitmap) {
        byte[] bitmapBytes = bitmapToByteArray(bitmap);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(bitmapBytes);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }
}
