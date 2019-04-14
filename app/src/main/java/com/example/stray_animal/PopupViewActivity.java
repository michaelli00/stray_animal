package com.example.stray_animal;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.mapping.popup.Popup;
import com.esri.arcgisruntime.mapping.popup.PopupAttachment;
import com.esri.arcgisruntime.mapping.popup.PopupAttachmentManager;
import com.esri.arcgisruntime.mapping.popup.PopupField;
import com.esri.arcgisruntime.mapping.popup.PopupManager;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class PopupViewActivity extends AppCompatActivity {

    private TextView textView;
    private Popup popup;
    private PopupManager popupManager;
    private PopupAttachmentManager popupAttachmentManager;
    private Bitmap bitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textView = findViewById(R.id.conditionView);

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

                } catch (InterruptedException | ExecutionException ex) {
                    // must deal with checked exceptions thrown from the async identify operation
                    ex.printStackTrace();
                }

                if(popupAttachments.size() > 0){
                    ListenableFuture<Bitmap> bitmapListenableFuture = popupAttachments.get(0).createFullImageAsync();
                    bitmapListenableFuture.addDoneListener(new Runnable() {
                        @Override
                        public void run() {
                            bitmap = null;
                            try {
                                // get the identify results from the future - returns when the operation is complete
                                bitmap = bitmapListenableFuture.get();

                            } catch (InterruptedException | ExecutionException ex) {
                                // must deal with checked exceptions thrown from the async identify operation
                                ex.printStackTrace();
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
            textView.setText(popupManager.getFormattedValue(condition));
        }


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

}
