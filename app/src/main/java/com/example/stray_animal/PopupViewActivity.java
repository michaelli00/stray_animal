package com.example.stray_animal;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Attachment;
import com.esri.arcgisruntime.mapping.popup.Popup;
import com.esri.arcgisruntime.mapping.popup.PopupAttachment;
import com.esri.arcgisruntime.mapping.popup.PopupAttachmentManager;
import com.esri.arcgisruntime.mapping.popup.PopupField;
import com.esri.arcgisruntime.mapping.popup.PopupManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PopupViewActivity extends AppCompatActivity {

    private TextView conditionView;
    private TextView speciesView;
    private TextView descriptionView;
    private Popup popup;
    private PopupManager popupManager;
    private PopupAttachmentManager popupAttachmentManager;
    private Bitmap bitmap;
    private ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        conditionView = findViewById(R.id.conditionView);
        speciesView = findViewById(R.id.speciesView);
        descriptionView = findViewById(R.id.descriptionView);
        imageView = findViewById(R.id.imageView);
        popup = MainActivity.currentPopup;
        this.setTitle(popup.getTitle());
        popupManager = new PopupManager(this, popup);
        popupAttachmentManager = popupManager.getAttachmentManager();
        ListenableFuture<List<PopupAttachment>> listenableFuture = popupAttachmentManager.fetchAttachmentsAsync();
        listenableFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                List<PopupAttachment> popupAttachments = null;
                try {
                    // get the identify results from the future - returns when the operation is complete
                    popupAttachments = listenableFuture.get();
                    Log.d("popupAttachments", "set popup attachments, size is " + popupAttachments.size());

                } catch (InterruptedException | ExecutionException ex) {
                    // must deal with checked exceptions thrown from the async identify operation
                    ex.printStackTrace();
                }

                if(popupAttachments.size() > 0){
                    Attachment attachment = popupAttachments.get(0).getAttachment();
                    Log.d("Attachment Size", "Attachment size not 0");
                    ListenableFuture<InputStream> attachmentListenableFuture = attachment.fetchDataAsync();
                    attachmentListenableFuture.addDoneListener(new Runnable() {
                        @Override
                        public void run() {
                            bitmap = null;
                            try {
                                // get the identify results from the future - returns when the operation is complete
                                bitmap = BitmapFactory.decodeStream(attachmentListenableFuture.get());
                                Log.d("bitmap", "Setting bitmap");
                                Log.d("bitmap", bitmap.getWidth() + "");
                                imageView.setImageBitmap(bitmap);

                            } catch (InterruptedException | ExecutionException ex) {
                                // must deal with checked exceptions thrown from the async identify operation
                                ex.printStackTrace();
                            }
                            if(bitmap == null){
                                Log.d("bitmap", "bitmap is null");
                            }
                        }
                    });
                }

            }
        });

        List<PopupField> popupFields = popupManager.getDisplayedFields();
        PopupField condition = null, species = null, description = null;
        for(PopupField p : popupFields) {
            String name = p.getFieldName();
            if(name.equals("condition")){
                condition = p;
            } else if(name.equals("species")){
                species = p;
            } else if(name.equals("description")){
                description = p;
            }
        }
        if(condition != null){
            conditionView.setText(popupManager.getFormattedValue(condition));
        }
        if(species != null){
            speciesView.setText(popupManager.getFormattedValue(species));
        }
        if(description != null){
            descriptionView.setText(popupManager.getFormattedValue(description));
        }
        if(bitmap != null){
//            byte[] decodedString = Base64.decode(person_object.getPhoto(),Base64.NO_WRAP);
//            InputStream inputStream  = new ByteArrayInputStream(decodedString);
//            Bitmap bitmap  = BitmapFactory.decodeStream(inputStream);
            imageView.setImageBitmap(bitmap);
        }
    }

}
