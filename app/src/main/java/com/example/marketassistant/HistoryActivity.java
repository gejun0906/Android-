package com.example.marketassistant;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 检测历史记录页面
 */
public class HistoryActivity extends AppCompatActivity {

    private static final int REQUEST_DETAIL = 100;

    private RecyclerView recyclerView;
    private HistoryAdapter historyAdapter;
    private List<DetectionRecord> recordList;
    private DetectionDbHelper dbHelper;
    private LinearLayout layoutEmpty;
    private TextView tvRecordCount;
    private Spinner spinnerFilter;
    private Handler handler;

    private String currentFilter = null; // null = 全部
    private String currentSearch = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.history_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new DetectionDbHelper(this);
        handler = new Handler(Looper.getMainLooper());
        initViews();
        setupRecyclerView();
        setupFilter();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view_history);
        layoutEmpty = findViewById(R.id.layout_empty);
        tvRecordCount = findViewById(R.id.tv_record_count);
        spinnerFilter = findViewById(R.id.spinner_filter);
    }

    private void setupRecyclerView() {
        recordList = new ArrayList<>();
        historyAdapter = new HistoryAdapter(this, recordList);

        historyAdapter.setOnItemClickListener(new HistoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(DetectionRecord record) {
                Intent intent = new Intent(HistoryActivity.this, HistoryDetailActivity.class);
                intent.putExtra("record", record);
                startActivityForResult(intent, REQUEST_DETAIL);
            }
        });

        historyAdapter.setOnItemLongClickListener(new HistoryAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(DetectionRecord record, int position) {
                showDeleteDialog(record, position);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(historyAdapter);
    }

    private void setupFilter() {
        // 初始加载时先显示"全部水果"
        updateFilterSpinner();
    }

    private void updateFilterSpinner() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<String> types = dbHelper.getDistinctFruitTypes();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        List<String> filterOptions = new ArrayList<>();
                        filterOptions.add(getString(R.string.filter_all));
                        filterOptions.addAll(types);

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                HistoryActivity.this,
                                android.R.layout.simple_spinner_item,
                                filterOptions);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerFilter.setAdapter(adapter);

                        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                if (position == 0) {
                                    currentFilter = null;
                                } else {
                                    currentFilter = filterOptions.get(position);
                                }
                                loadRecords();
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                            }
                        });
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecords();
    }

    private void loadRecords() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<DetectionRecord> records;
                if (currentSearch != null && !currentSearch.isEmpty()) {
                    records = dbHelper.searchRecords(currentSearch);
                } else if (currentFilter != null) {
                    records = dbHelper.getRecordsByFruitType(currentFilter);
                } else {
                    records = dbHelper.getAllRecords();
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recordList.clear();
                        recordList.addAll(records);
                        historyAdapter.notifyDataSetChanged();
                        updateEmptyState();
                    }
                });
            }
        }).start();
    }

    private void loadRecordsSortedByScore() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<DetectionRecord> records = dbHelper.getRecordsSortedByScore(false);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recordList.clear();
                        recordList.addAll(records);
                        historyAdapter.notifyDataSetChanged();
                        updateEmptyState();
                    }
                });
            }
        }).start();
    }

    private void updateEmptyState() {
        if (recordList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        tvRecordCount.setText(String.format(getString(R.string.history_record_count), recordList.size()));
    }

    private void showDeleteDialog(final DetectionRecord record, final int position) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.history_confirm_delete)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                dbHelper.deleteRecord(record.getId());
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        recordList.remove(position);
                                        historyAdapter.notifyItemRemoved(position);
                                        updateEmptyState();
                                        Toast.makeText(HistoryActivity.this,
                                                R.string.history_deleted, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }).start();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showClearAllDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_clear_all)
                .setMessage(R.string.history_confirm_clear)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                dbHelper.clearAllRecords();
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        recordList.clear();
                                        historyAdapter.notifyDataSetChanged();
                                        updateEmptyState();
                                        updateFilterSpinner();
                                        Toast.makeText(HistoryActivity.this,
                                                R.string.history_cleared, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }).start();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ========== 菜单 ==========

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);

        // 搜索功能
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.action_search));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    currentSearch = query;
                    loadRecords();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText.isEmpty()) {
                        currentSearch = null;
                        loadRecords();
                    }
                    return true;
                }
            });
        }

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                currentSearch = null;
                loadRecords();
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sort_time) {
            currentSearch = null;
            loadRecords();
            return true;
        } else if (id == R.id.action_sort_score) {
            loadRecordsSortedByScore();
            return true;
        } else if (id == R.id.action_clear_all) {
            showClearAllDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DETAIL && resultCode == RESULT_OK) {
            loadRecords();
            updateFilterSpinner();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
