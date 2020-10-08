package com.ojasvitaa.drawingapp

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.Manifest
import kotlin.Exception

class MainActivity : AppCompatActivity() {

    private var mImageButtonCurrentPaint: ImageButton?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawing_view.setSizeForBrush(20.toFloat())

        mImageButtonCurrentPaint= ll_paint_colors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_selected)
        )

        ib_brush.setOnClickListener{
            showBrushSizeChooserDialog()
        }

        ib_gallery.setOnClickListener{
            if (isReadSrorageAllowed()){
                 val pickPhotoIntent= Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                startActivityForResult(pickPhotoIntent,GALLERY)

            }else{
                requestStoragePermission()
            }
        }

        ib_undo.setOnClickListener {
            drawing_view.onClickUndo()
        }

        ib_save.setOnClickListener {
            if (isReadSrorageAllowed()){
                BitmapAsynTask(getBitmapFromView(fl_drawing_view_container)).execute()
            }else{
                requestStoragePermission()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode== Activity.RESULT_OK){
            if (requestCode== GALLERY){
                try {
                    if (data!!.data != null){
                        iv_background.visibility= View.VISIBLE
                        iv_background.setImageURI(data.data)
                    }else{
                        Toast.makeText(this@MainActivity,"Error occured. ",Toast.LENGTH_LONG).show()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog= Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        val smallBtn= brushDialog.ib_small_brush
        smallBtn.setOnClickListener{
            drawing_view.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn= brushDialog.ib_medium_brush
        mediumBtn.setOnClickListener{
            drawing_view.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn= brushDialog.ib_large_brush
        largeBtn.setOnClickListener{
            drawing_view.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun  paintClicked(view: View){
        if(view !== mImageButtonCurrentPaint){
            val imageButton= view as ImageButton

            val colorTag = imageButton.tag.toString()
            drawing_view.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_selected)
            )
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )
            mImageButtonCurrentPaint=view

        }
    }

    private fun requestStoragePermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){
            Toast.makeText(this@MainActivity,"Need permission to add a background ",Toast.LENGTH_LONG).show()
        }
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest .permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode== STORAGE_PERMISSION_CODE){
            if (grantResults.isNotEmpty()&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this@MainActivity,"Permission granted now you can read storage files ",Toast.LENGTH_LONG).show()
            }
            else{
                Toast.makeText(this@MainActivity,"You Denied the Permission ",Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun isReadSrorageAllowed():Boolean{
        val result= ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)

        return result== PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width,view.height, Bitmap.Config.ARGB_8888)

        val canvas= Canvas(returnedBitmap)
        val bgDrawable= view.background
        if (bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }

    private inner class BitmapAsynTask(val mBitmap: Bitmap):
        AsyncTask<Any, Void, String >() {

        override fun doInBackground(vararg p0: Any?): String {
            var result = ""

            if (mBitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(externalCacheDir!!.absoluteFile.toString()
                            + File.separator + "DrawingApp_"
                            + System.currentTimeMillis() / 1000 +".png")

                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result= f.absolutePath


                }catch (e : Exception){
                    result= ""
                    e.printStackTrace()
                }
            }

            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(!result!!. isEmpty()){
                Toast.makeText(this@MainActivity, "File saved successfully : $result",Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(this@MainActivity, "Something went wrong while saving the file",Toast.LENGTH_LONG).show()
            }
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result),null){
                path,uri ->val shareIntent= Intent()
                shareIntent.action= Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/png"

                startActivity(
                    Intent.createChooser(
                        shareIntent,"Share"
                    )
                )
            }
        }

    }

    companion object{
        private const val STORAGE_PERMISSION_CODE= 1
        private const val GALLERY=2
    }
}