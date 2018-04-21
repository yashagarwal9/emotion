package com.example.yashagarwal.emotion;
        import android.Manifest;
        import android.annotation.SuppressLint;
        import android.annotation.TargetApi;
        import android.app.ProgressDialog;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.graphics.Bitmap;
        import android.graphics.BitmapFactory;
        import android.net.Uri;
        import android.os.AsyncTask;
        import android.os.Build;
        import android.support.annotation.NonNull;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.view.View;
        import android.widget.Button;
        import android.widget.ImageView;
        import android.widget.Toast;

        import com.microsoft.projectoxford.emotion.EmotionServiceClient;
        import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
        import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
        import com.microsoft.projectoxford.emotion.contract.Scores;
        import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;

        import java.io.ByteArrayInputStream;
        import java.io.ByteArrayOutputStream;
        import java.io.FileNotFoundException;
        import java.io.IOException;
        import java.io.InputStream;
        import java.util.ArrayList;
        import java.util.Collection;
        import java.util.Collections;
        import java.util.List;

public class MainActivity extends AppCompatActivity {
    ImageView imageView;
    Button btnTakePicture, btnProcess;
    public EmotionServiceClient restClient= new EmotionServiceRestClient("e11b877be829455f8cecbc2acfa6bc11");
    int TAKE_PICTURE_CODE = 100, REQUEST_PERMISSION_CODE = 101;
    Bitmap mBitmap;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_PERMISSION_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    @android.support.annotation.RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET
            }, REQUEST_PERMISSION_CODE);
        }
    }

    private void initViews() {
        btnProcess = (Button)findViewById(R.id.btnProcess);
        btnTakePicture = (Button)findViewById(R.id.btnTakePic);
        imageView = (ImageView)findViewById(R.id.imageView);

        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicFromGallery();
            }
        });

        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processImage();
            }
        });
    }

    public void processImage() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        AsyncTask<InputStream,String,List<RecognizeResult>> processAsync = new AsyncTask<InputStream,String,List<RecognizeResult>>(){

            ProgressDialog mDialog = new ProgressDialog(MainActivity.this);

            @Override
            protected void onPreExecute() {
                mDialog.show();
            }

            @Override
            protected void onProgressUpdate(String... values) {
                mDialog.setMessage(values[0]);
            }

            @Override
            protected List<RecognizeResult> doInBackground(InputStream... params) {
                publishProgress("Please wait...");
                List<RecognizeResult> result = null;
                try {
                    result = restClient.recognizeImage(params[0]);
                } catch (EmotionServiceException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return result;
            }

            @Override
            protected void onPostExecute(List<RecognizeResult> recognizeResults) {
                mDialog.dismiss();
                for(RecognizeResult res : recognizeResults){
                    String status = getEmotion(res);
                    imageView.setImageBitmap(ImageHelper.drawRectOnBitmap(mBitmap,res.faceRectangle,status));
                }
            }
        };
        processAsync.execute(inputStream);
    }

    private String getEmotion(RecognizeResult res) {
        List<Double> list = new ArrayList<>();
        Scores scores = res.scores;
        list.add(scores.anger);
        list.add(scores.happiness);
        list.add(scores.contempt);
        list.add(scores.disgust);
        list.add(scores.fear);
        list.add(scores.neutral);
        list.add(scores.sadness);
        list.add(scores.surprise);

        Collections.sort(list);

        double maxNum = list.get(list.size() - 1);

        if(maxNum == scores.anger)
            return "Anger";
        else if(maxNum == scores.happiness)
            return "Happiness";
        else if(maxNum == scores.contempt)
            return "Contempt";
        else if(maxNum == scores.disgust)
            return "Disgust";
        else if(maxNum == scores.fear)
            return "Fear";
        else if(maxNum == scores.neutral)
            return "Neutral";
        else if(maxNum == scores.sadness)
            return "Sadness";
        else if(maxNum == scores.surprise)
            return "Surprise";
        else
            return "Can't Detect";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == TAKE_PICTURE_CODE){
            Uri selectedImageUri = data.getData();
            InputStream in = null;
            try {
                in = getContentResolver().openInputStream(selectedImageUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            mBitmap = BitmapFactory.decodeStream(in);
            imageView.setImageBitmap(mBitmap);;

        }
    }

    private void takePicFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, TAKE_PICTURE_CODE);
    }
}
