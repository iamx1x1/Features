package com.example.mandyyang.features;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.Manifest;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;



public class MainActivity extends AppCompatActivity {
    public  static final String TAG = "OpenCV";

    private Mat originalMat;
    private Bitmap currentBitmap,processedBitmap;
    private static final int ACTION_PICK_PHOTO = 1;
    static int REQUEST_READ_EXTERNAL_STORAGE = 0;
    private ImageView originalView, processedView;

    static boolean read_external_storage_granted = false;

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                    Log.i("OpenCV","OpenCV loaded successfully.");
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ACTION_PICK_PHOTO && resultCode == RESULT_OK && null != data && read_external_storage_granted){
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            String picturePath;

            if (cursor == null){
                Log.i("data", "cannot load image");
                return;
            }else{
                try{
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    picturePath = cursor.getString(columnIndex);
                }finally {
                    cursor.close();
                }
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap temp = BitmapFactory.decodeFile(picturePath, options);

            int orientation = 0;

            try{
                ExifInterface imgParams = new ExifInterface(picturePath);
                orientation = imgParams.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Matrix rotate90 = new Matrix();
            rotate90.postRotate(orientation);
            Bitmap originalBitmap = rotateBitmap(temp, orientation);

            if (originalBitmap != null){
                Bitmap tempBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                originalMat=new Mat(tempBitmap.getHeight(), tempBitmap.getWidth(), CvType.CV_8U);
                Utils.bitmapToMat(tempBitmap, originalMat);
                currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888,false);
                originalView.setImageBitmap(currentBitmap);
            }else{
                Log.i("data", "originalBitmap is empty");
            }
        }
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation){
        Matrix matrix = new Matrix();
        switch(orientation){
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1,1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1,1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1,1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1,1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }catch(OutOfMemoryError e){
            e.printStackTrace();
            return null;
        }
    }




    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults){
        if(requestCode == REQUEST_READ_EXTERNAL_STORAGE){
            if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.i("permission", "READ_EXTERNAL_STORAGE granted");
                read_external_storage_granted = true;
            }else{
                Log.i("permission", "READ_EXTERNAL_STORAGE denied");
            }
        }
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this,mOpenCVCallBack);

        originalView = (ImageView)findViewById(R.id.originalImage);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            Log.i("permission", "request READ_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
        }else{
            Log.i("permission","READ_EXTERNAL_STORAGE already granted");
            read_external_storage_granted = true;
        }
    }

    protected void onResume(){
        super.onResume();
    }

    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if(id == R.id.open_gallery){

            if(read_external_storage_granted){
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, ACTION_PICK_PHOTO);
            }else{
                Log.i("OpenCV","read_external_storage_granted=0");
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}

