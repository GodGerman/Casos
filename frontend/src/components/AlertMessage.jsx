import React from 'react';

const ICONS = {
  success: <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>,
  danger: <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>,
  warning: <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>,
  info: <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
};

export default function AlertMessage({ type = 'info', message }) {
  if (!message) return null;

  const icon = ICONS[type] || ICONS.info;
  const alertClass = type === 'danger' ? 'alert-danger' :
    type === 'success' ? 'alert-success' :
      type === 'warning' ? 'alert-warning' : 'alert-info';

  return (
    <div className={`alert ${alertClass} d-flex align-items-start gap-3 shadow-sm border-0`} role="alert">
      <div className="flex-shrink-0 mt-1 opacity-75">{icon}</div>
      <div className="flex-grow-1 text-sm">{message}</div>
    </div>
  );
}
