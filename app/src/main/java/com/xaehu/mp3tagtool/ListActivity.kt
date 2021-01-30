package com.xaehu.mp3tagtool

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_list.*
import java.util.*


class ListActivity : AppCompatActivity() {
    val TAG = "ListActivity"
    lateinit var allList:MutableList<SongBean>
    val adapter =  MyListAdapter(ArrayList())
    var position:Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        title = "本地歌曲"
        allList = getMusicList()
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)
        adapter.setListener(object : MyListAdapter.OnListener {
            override fun setOnItemClickListener(bean: SongBean,position:Int) {
                this@ListActivity.position = position
                val intent = Intent(this@ListActivity, MatchActivity::class.java)
                intent.putExtra("data", bean)
                startActivityForResult(intent,1000)
            }
        })
        adapter.setNewData(allList)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.search, menu)
        // 获得menu中指定的菜单项
        val item: MenuItem = menu.findItem(R.id.action_search)
        // 获得菜单项中的SearchView
        val searchView: SearchView = item.actionView as SearchView
        // 当SearchView获得焦点时弹出软键盘的类型，就是设置输入类型
        searchView.inputType = EditorInfo.TYPE_CLASS_TEXT
        // 设置回车键表示查询操作
        searchView.imeOptions = EditorInfo.IME_ACTION_SEARCH
        //设置最右侧的提交按钮
        searchView.isSubmitButtonEnabled = true;
        searchView.queryHint = "歌手或歌名检索"
        // 为searchView添加事件
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // 输入后点击回车改变文本
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchList(query)
                return false
            }

            // 随着输入改变文本
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        searchView.setOnCloseListener {
            searchView.clearFocus()
            searchView.onActionViewCollapsed()
            searchList(null)
            title = "本地歌曲"
            true
        }
        searchView.setOnSearchClickListener {
            title = "搜索"
        }

        return true
    }

    private fun searchList(query: String?) {
        val list:MutableList<SongBean> = ArrayList()
        if(TextUtils.isEmpty(query)){
            adapter.setNewData(allList)
            return
        }
        for(item in allList){
            item.apply {
                if(name?.contains(query!!) == true || singer?.contains(query!!) == true){
                    list.add(this)
                }
            }
        }
        adapter.setNewData(list)
    }


    /**
     * 得到媒体的音乐文件列表
     */
    private fun getMusicList(): MutableList<SongBean> {
        val list: MutableList<SongBean> = ArrayList()
        val cursor: Cursor? = this.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            MediaStore.Audio.AudioColumns.IS_MUSIC
        )
        if (cursor != null) {
            var song: SongBean
            while (cursor.moveToNext()) {
                var singer = ""
                var name = ""
                val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val split = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                    .split("-")
                val size = split.size
                when {
                    size == 1 -> {
                        name = split[0]
                    }
                    size == 2 -> {
                        singer = split[0]
                        name = split[1]
                    }
                    size > 2 -> {
                        name = split.last()
                        for (i in 0 until size-1){
                            singer += " "+split[i]
                        }
                    }
                }
                song = SongBean(name.replace(".mp3", "").trim(), singer.trim(), path)
                list.add(song)
            }
            cursor.close()
        }
        list.reverse()
        return list
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 1000 && resultCode == 1001){
            adapter.notifyItemChanged(position)
        }
    }

}