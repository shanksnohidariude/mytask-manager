/**
 * タスク削除処理
 */
function deleteTask() {
    if (confirm('本当に削除しますか？\nこの操作は取り消せません。')) {
        document.getElementById('deleteForm').submit();
    }
}