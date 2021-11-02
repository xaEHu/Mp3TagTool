package com.xaehu.mp3tagtool

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.android.synthetic.main.activity_match.*
import okhttp3.*
import org.jaudiotagger.audio.mp3.MP3File
import org.jaudiotagger.tag.images.ArtworkFactory
import org.jetbrains.anko.alert
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MatchActivity : AppCompatActivity() {
    private val TAG = "MatchActivity"
    private val adapter = MyListAdapter(ArrayList())
    lateinit var path:String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match)
        title = "匹配专辑图"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)
        adapter.setListener(object : MyListAdapter.OnListener {
            override fun setOnItemClickListener(bean: SongBean,position:Int) {
                alert {
                    setTitle("写入专辑图")
                    negativeButton("下次一定") {}
                    positiveButton("写入") {
                        val request = ImageRequest.Builder(this@MatchActivity)
                            .data(bean.path)
                            .target { drawable ->
                                // Handle the result.
                                val bitmapDrawable: BitmapDrawable = drawable as BitmapDrawable
                                val bitmap: Bitmap = bitmapDrawable.bitmap
                                val filePath = "${filesDir.absoluteFile}/temp.jpg";
                                Log.i(TAG, "filePath: $filePath")
                                val file = bitmapToFile(
                                    filePath,
                                    bitmap, 80
                                )
                                Log.i(TAG, "BitmapFilePath: ${file?.absoluteFile}")
                                writeTag(path, File(filePath))
                            }
                            .build()
                        imageLoader.enqueue(request)
                    }
                }.show()
            }
        })

        val songBean = intent.getParcelableExtra<SongBean>("data")
        this.path = songBean?.path!!
        songBean.run {
            et_name.setText(name ?: "获取数据失败")
            et_singer.setText(singer)
            et_path.setText(path)
            search(name, singer)
        }
        btn_search.setOnClickListener {
            search(et_name.text.toString(), et_singer.text.toString())
        }

        btnSelect.setOnClickListener{
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(intent,1)
        }

    }

    fun search(name: String?, singer: String?) {
        btn_search.isEnabled = false
        btn_search.text = "搜索中……"
        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(singer)) {
            Toast.makeText(this, "请输入歌曲信息", Toast.LENGTH_SHORT).show()
            return
        }
        val word = singer?.trim() + name?.trim()
        val url = "http://music.163.com/api/search/pc?csrf_token=hlpretag=&hlposttag=&s=$word&type=1&offset=0&total=true&limit=20"
        Log.i(TAG, "search: $url")

        val okHttpClient = OkHttpClient()
        val request: Request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .addHeader("Cookie", "NMTID="+System.currentTimeMillis())
            .build()
        val call = okHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //失败处理
                btn_search.isEnabled = true
                btn_search.text = "搜索"
                Toast.makeText(applicationContext, "请求失败", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonObject = JSONObject(response.body()!!.string())
                Log.i(TAG, "onResponse: $jsonObject")
                runOnUiThread {
                    btn_search.isEnabled = true
                    btn_search.text = "搜索"
                    val code: Int = jsonObject.getInt("code")
                    if (code == 200) {
                        val result:JSONObject? = jsonObject.getJSONObject("result");
                        val jsonArray:JSONArray? = try {
                            result?.getJSONArray("songs")
                        }catch (e:Exception){
                            null
                        }
                        if(jsonArray == null){
                            Toast.makeText(applicationContext, "什么都没有搜到", Toast.LENGTH_SHORT).show()
                            return@runOnUiThread
                        }
                        val list = ArrayList<SongBean>()
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.get(i) as JSONObject
                            val singerArr = item.getJSONArray("artists")
                            var singerStr = ""
                            for (j in 0 until singerArr.length()){
                                val singerObj = singerArr.get(j) as JSONObject
                                singerStr += singerObj.get("name")
                                singerStr += " "
                            }
                            list.add(
                                SongBean(
                                    item.getString("name"),
                                    singerStr,
                                    item.getJSONObject("album").getString("picUrl")
                                )
                            )
                        }
                        adapter.setNewData(list)
                        if (adapter.itemCount == 0) {
                            Toast.makeText(applicationContext, "什么都没有搜到", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        var msg = jsonObject.getString("message")
                        if(msg.isEmpty()){
                            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                        }else{
                            Toast.makeText(applicationContext, "服务器数据错误", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun writeTag(path: String?, picFile: File?) {
        val mp3File = MP3File(path)
            mp3File.run {
                val artWork = ArtworkFactory.createArtworkFromFile(picFile)
                when {
                    hasID3v2Tag() -> {
                        iD3v2Tag.setField(artWork)
                    }
                    hasID3v1Tag() -> {
//                        iD3v1Tag.setField(artWork) 这个方法行不通
                        Toast.makeText(applicationContext, "暂不支持ID3v1Tag歌曲", Toast.LENGTH_SHORT).show()
                        return
                    }
                    else -> {
                        Toast.makeText(applicationContext, "此歌曲没有ID3Tag", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
                save()
                Toast.makeText(applicationContext, "写入完成", Toast.LENGTH_SHORT).show()
                setResult(1001)
            }
    }

    /**
     * bitmap保存为file
     */
    @Throws(IOException::class)
    fun bitmapToFile(
        filePath: String,
        bitmap: Bitmap?, quality: Int
    ) : File? {
        if (bitmap != null) {
            val file = File(
                filePath.substring(
                    0,
                    filePath.lastIndexOf(File.separator)
                )
            )
            if (!file.exists()) {
                file.mkdirs()
            }
            val bos = BufferedOutputStream(
                FileOutputStream(filePath)
            )
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
            bos.flush()
            bos.close()
            return file
        }
        return null
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && null != data) {
            val selectedImage: Uri? = data.data
            val filePathColumn =
                arrayOf(MediaStore.Images.Media.DATA)
            //查询我们需要的数据
            val cursor: Cursor? = contentResolver.query(
                selectedImage!!,
                filePathColumn, null, null, null
            )
            cursor?.moveToFirst()
            val columnIndex: Int = cursor?.getColumnIndex(filePathColumn[0])!!
            val picturePath = cursor.getString(columnIndex)
            cursor.close()

            alert("\n将选择的图片作为专辑图写入该歌曲中？", "写入专辑图") {
                negativeButton("下次一定") {}
                positiveButton("写入") {
                    writeTag(path, File(picturePath))
                }
            }.show()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}