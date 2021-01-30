package com.xaehu.mp3tagtool

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import coil.imageLoader
import coil.load
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
import java.lang.Exception


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
                val url = bean.path
                val img = ImageView(applicationContext)
                img.load(url)
                alert("写入专辑图", "确定写入吗？") {
                    negativeButton("下次一定") {}
                    positiveButton("写入") {
//                        indeterminateProgressDialog("正在写入")
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
                        val disposable = imageLoader.enqueue(request)
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
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
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
                        Toast.makeText(applicationContext, "服务器数据错误", Toast.LENGTH_SHORT).show()
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
//        val picFile = File(url!!)
        val mp3File = MP3File(path)
        if (mp3File.hasID3v2Tag()) {
            Log.i(TAG, "writeTag: ")
            mp3File.run {
                Log.i(TAG, "writeTag: run")
                val artWork = ArtworkFactory.createArtworkFromFile(picFile)
                iD3v2Tag.setField(artWork)
                save()
                Toast.makeText(applicationContext, "写入完成", Toast.LENGTH_SHORT).show()
                setResult(1001)
            }

        }else{
            Toast.makeText(applicationContext, "此歌曲没有ID3v2Tag", Toast.LENGTH_SHORT).show()
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

}