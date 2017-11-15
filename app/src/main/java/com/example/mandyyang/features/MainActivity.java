package com.example.mandyyang.features;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.annotation.NonNull;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MainActivity extends AppCompatActivity {
    public static final String TAG = "OpenCV";
    private Mat originalMat;
    private Bitmap currentBitmap, processedBitmap;
    private ImageView originalView, processedView;
    static int REQUEST_READ_EXTERNAL_STORAGE = 0;
    static boolean read_external_storage_granted = false;
    private final int ACTION_PICK_PHOTO = 1;

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    //DO YOUR WORK/STUFF HERE
                    Log.i("OpenCV", "OpenCV loaded successfully.");
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mOpenCVCallBack);
        originalView = (ImageView) findViewById(R.id.originalImage);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("permission", "request READ_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            Log.i("permission", "READ_EXTERNAL_STORAGE already granted");
            read_external_storage_granted = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.open_gallery) {
            if (read_external_storage_granted) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, ACTION_PICK_PHOTO);
            } else {
                Log.i("OenpCV", "read_external_storage_granted = false");
                return true;
            }
        } else if (id == R.id.DoG) {
            processedView = (ImageView) findViewById(R.id.ProcessedImage);
            //Apply Difference of Gaussian
            DifferenceOfGaussian();
            //Image_Canny
        } else if (id == R.id.CannyEdges) {
            processedView = (ImageView) findViewById(R.id.ProcessedImage);
            //Apply Difference of Gaussian
            Canny();

        } else if (id == R.id.SobelFilter) {
            processedView = (ImageView) findViewById(R.id.ProcessedImage);
            //Apply Difference of Gaussian
            Sobel();

        } else if (id == R.id.HarrisCorners) {
            processedView = (ImageView) findViewById(R.id.ProcessedImage);
            //Apply Difference of Gaussian
            HarrisCorner();

        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_PICK_PHOTO && resultCode == RESULT_OK && null != data && read_external_storage_granted) {

            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            String picturePath;
            if (cursor == null) {
                Log.i("data", "cannot load any image");
                return;
            } else {
                try {
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    picturePath = cursor.getString(columnIndex);
                } finally {
                    cursor.close();
                }
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap temp = BitmapFactory.decodeFile(picturePath, options);

            int orientation = 0;
            try {
                ExifInterface imgParams = new ExifInterface(picturePath);
                orientation = imgParams.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Matrix rotate90 = new Matrix();
            rotate90.postRotate(orientation);
            Bitmap originalBitmap = rotateBitmap(temp, orientation);

            if (originalBitmap != null) {
                Bitmap tempBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                originalMat = new Mat(tempBitmap.getHeight(),
                        tempBitmap.getWidth(), CvType.CV_8U);
                Utils.bitmapToMat(tempBitmap, originalMat);
                currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, false);
                originalView.setImageBitmap(currentBitmap);
            } else {
                Log.i("data", "originalBitmap is empty");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                Log.i("permission", "READ_EXTERNAL_STORAGE granted");
                read_external_storage_granted = true;
            } else {
                // permission denied
                Log.i("permission", "READ_EXTERNAL_STORAGE denied");
            }
        }
    }

    //rotateBitmap
    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    //Difference of Gaussian
    public void DifferenceOfGaussian() {
        Mat grayMat = new Mat();
        Mat blur1 = new Mat();
        Mat blur2 = new Mat();

        //Converting the image to grayscale
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY);

        Imgproc.GaussianBlur(grayMat, blur1, new Size(15, 15), 5);
        Imgproc.GaussianBlur(grayMat, blur2, new Size(21, 21), 5);

        //Subtracting the two blurred images
        Mat DoG = new Mat();
        Core.absdiff(blur1, blur2, DoG);

        //Inverse Binary Thresholding
        Core.multiply(DoG, new Scalar(100), DoG);
        Imgproc.threshold(DoG, DoG, 50, 255, Imgproc.THRESH_BINARY_INV);

        // processedBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888,ture);
        processedBitmap = Bitmap.createBitmap(currentBitmap.getWidth(), currentBitmap.getHeight(), currentBitmap.getConfig());
        //Converting Mat back to Bitmap
        Utils.matToBitmap(DoG, processedBitmap);
        processedView.setImageBitmap(processedBitmap);
    }

    //Canny Edge Detection_
    void Canny() {
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
//Converting the image to grayscale
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(grayMat, cannyEdges, 10, 100);
        processedBitmap = Bitmap.createBitmap(currentBitmap.getWidth(), currentBitmap.getHeight(), currentBitmap.getConfig()); //Converting Mat back to Bitmap
        Utils.matToBitmap(cannyEdges, processedBitmap);
        processedView.setImageBitmap(processedBitmap);

    }

    void Sobel() {
        Mat grayMat = new Mat();
        Mat sobel = new Mat(); //Mat to sote the final result

        //Matrices to store and absolute gradient respectively
        Mat grad_x = new Mat();
        Mat abs_grad_x = new Mat();

        Mat grad_y = new Mat();
        Mat abs_grad_y = new Mat();

        //Converting the image to grayscale
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_RGB2BGR);

        //Calculatin gradient in horizontal direction
        Imgproc.Sobel(grayMat, grad_x, CvType.CV_16S, 1, 0, 3, 1, 0);

        //Calculatin gradient in vertical direction
        Imgproc.Sobel(grayMat, grad_y, CvType.CV_16S, 0, 1, 3, 1, 0);

        //Calculating absolute value of gradients in both the direction
        Core.convertScaleAbs(grad_x, abs_grad_x);
        Core.convertScaleAbs(grad_y, abs_grad_y);

        //Calculating the resultant gradient
        Core.addWeighted(abs_grad_x, 0.5, abs_grad_y, 0.5, 1, sobel);

        // processedBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888,ture);
        processedBitmap = Bitmap.createBitmap(currentBitmap.getWidth(), currentBitmap.getHeight(), currentBitmap.getConfig());
        //Converting Mat back to Bitmap
        Utils.matToBitmap(sobel, processedBitmap);
        processedView.setImageBitmap(processedBitmap);
    }

    //HarrisCorner
    void HarrisCorner() {

        Mat grayMat = new Mat();
        Mat corners = new Mat();
        // 找出角點
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        // Harris角点的输出
        Mat tempDst = new Mat();
        Imgproc.cornerHarris(grayMat, tempDst, 2, 3, 0.04);

        Mat tempDstNorm = new Mat();
        Core.normalize(tempDst, tempDstNorm, 0, 255, Core.NORM_MINMAX);
        Core.convertScaleAbs(tempDstNorm, corners);

// 在新的图像上绘制角点
        Random r = new Random();
        for (int i = 0; i < tempDstNorm.cols(); i++) {
            for (int j = 0; j < tempDstNorm.rows(); j++) {
                double[] value = tempDstNorm.get(j, i);
                if (value[0] > 150) {
                    Imgproc.circle(corners, new Point(i, j), 5, new Scalar(r.nextInt(255), 2));
                }
            }


            // Mat转Bitmap
            processedBitmap = Bitmap.createBitmap(currentBitmap.getWidth(), currentBitmap.getHeight(), currentBitmap.getConfig());
            Utils.matToBitmap(corners, processedBitmap);
            processedView.setImageBitmap(processedBitmap);


        }
        }
    }


