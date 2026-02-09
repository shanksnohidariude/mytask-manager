/**
 * ai_plan_confirm.js
 * - 削除時の確認ダイアログ
 * - 削除後の再インデックス（name属性・表示番号の振り直し）
 * - タスク数表示の更新、保存ボタンの無効化制御、空状態メッセージ表示
 * - 送信時の二重送信防止（簡易）
 */

/* DOM 準備時にインデックスとカウントを初期化 */
document.addEventListener('DOMContentLoaded', () => {
  updateTaskNumbers();
  updateTaskCount();

  // フォーム送信時の二重送信防止（簡易）
  const form = document.getElementById('taskForm') || document.querySelector('form');
  if (form) {
    form.addEventListener('submit', (e) => {
      const saveBtn = form.querySelector('.btn-save');
      if (saveBtn && !saveBtn.disabled) {
        // ボタンを無効化して表示を変更
        saveBtn.disabled = true;
        saveBtn.dataset.originalText = saveBtn.textContent;
        saveBtn.textContent = '送信中… ⏳';
        form.setAttribute('aria-busy', 'true');

        // 保険: 5秒後に自動で復帰（サーバ応答がない場合のフォールバック）
        setTimeout(() => {
          if (saveBtn) {
            saveBtn.disabled = false;
            if (saveBtn.dataset.originalText) saveBtn.textContent = saveBtn.dataset.originalText;
            form.removeAttribute('aria-busy');
          }
        }, 5000);
      } else {
        // 既に無効化されているなら多重送信を防ぐ
        e.preventDefault();
      }
    });
  }
});

/**
 * 削除ボタンから呼ばれる (HTML の onclick="deleteTask(this)" と互換)
 * @param {HTMLElement} button - クリックされた削除ボタン
 */
function deleteTask(button) {
  if (!button) return;

  // 確認ダイアログ
  if (!confirm('このタスクを削除してよいですか？')) return;

  const taskItem = button.closest('.task-item');
  if (!taskItem) return;

  taskItem.remove();

  // タスク番号と name 属性を振り直す
  updateTaskNumbers();

  // タスク数を更新（空状態処理なども行う）
  updateTaskCount();
}

/**
 * タスク番号・name属性の再インデックス
 * - 各 task-item を順に走査し、.task-number の表示と input/textarea の name を揃える
 * - name 属性は "tasks[<index>].suffix" のパターンを想定し、数値部分だけ差し替える
 */
function updateTaskNumbers() {
  const taskItems = document.querySelectorAll('.task-item');
  taskItems.forEach((item, index) => {
    // タスク番号の更新（.task-number が存在する前提）
    const numberSpan = item.querySelector('.task-number');
    if (numberSpan) numberSpan.textContent = (index + 1) + '.';

    // data-index 属性を更新（サーバー側で参照する場合に備えて）
    item.setAttribute('data-index', index);

    // input / textarea / select などの name を更新
    const inputs = item.querySelectorAll('input, textarea, select');
    inputs.forEach(input => {
      const name = input.getAttribute('name');
      if (name) {
        // tasks[0].title のような形式を想定して数値部分のみ置換
        const newName = name.replace(/tasks\[\d+\]/, 'tasks[' + index + ']');
        input.setAttribute('name', newName);
      }
    });
  });
}

/**
 * タスク数の表示更新と空状態のUI制御
 * - #task-count の更新
 * - .btn-save の無効化（タスクが0件のとき）
 * - 空状態メッセージ（.no-tasks）の挿入/削除
 */
function updateTaskCount() {
  const count = document.querySelectorAll('.task-item').length;

  // カウント表示を更新
  const countEl = document.getElementById('task-count');
  if (countEl) countEl.textContent = count;

  // 保存ボタンの有効/無効を切り替え
  const saveBtn = document.querySelector('.btn-save');
  if (saveBtn) saveBtn.disabled = (count === 0);

  // 空状態メッセージの表示（taskList の中に .no-tasks 要素を追加）
  const taskList = document.getElementById('taskList');
  if (!taskList) return;

  const existing = taskList.querySelector('.no-tasks');

  if (count === 0) {
    if (!existing) {
      const node = document.createElement('div');
      node.className = 'no-tasks';
      node.textContent = 'AI が生成したタスクはありません。キャンセルして戻るか、新しいタスクを作成してください。';
      // 適切な見た目のために最後に追加
      taskList.appendChild(node);
    }
  } else {
    if (existing) existing.remove();
  }
}

/* 互換性のためのエイリアス（既存コードや外部から呼ばれる可能性） */
const reindexTasks = updateTaskNumbers;

/* グローバルに公開（HTML の onclick からも参照可能にする） */
window.deleteTask = deleteTask;
window.updateTaskNumbers = updateTaskNumbers;
window.updateTaskCount = updateTaskCount;
window.reindexTasks = reindexTasks;