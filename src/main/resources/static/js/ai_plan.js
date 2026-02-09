// ai-plan.js (optional)
// - 最小限のUX改善: 二度送信防止・送信時のローディング表示・簡易のクライアント検証
// - HTML の編集は不要。スクリプトを有効にするには下記の <script> を追加してください。

document.addEventListener('DOMContentLoaded', () => {
  const form = document.querySelector('form[th\\:action], form[action="/tasks/generate-plan"], form');
  if(!form) return;

  const submitBtn = form.querySelector('button[type="submit"]');
  if(!submitBtn) return;

  // ボタンクリックでローディング状態にするユーティリティ
  function setLoading(on){
    if(on){
      submitBtn.disabled = true;
      submitBtn.dataset.original = submitBtn.textContent;
      submitBtn.textContent = '生成中… ⏳';
      form.setAttribute('aria-busy','true');
    }else{
      submitBtn.disabled = false;
      if(submitBtn.dataset.original) submitBtn.textContent = submitBtn.dataset.original;
      form.removeAttribute('aria-busy');
    }
  }

  // 簡易バリデーション: goal が空でないこと
  form.addEventListener('submit', (e) => {
    const goal = form.querySelector('#goal');
    if(goal && !goal.value.trim()){
      e.preventDefault();
      goal.focus();
      alert('目標を入力してください。');
      return;
    }

    // 二度送信防止 + ローディング表示
    setLoading(true);
    // フォールバック: 5秒後にボタン有効化（万が一）
    setTimeout(()=> setLoading(false), 5000);
  });
});