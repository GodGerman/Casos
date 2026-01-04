import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './src/App.jsx';
import 'bootstrap/dist/css/bootstrap.min.css';
import './Css/theme.css';
import './Css/app.css';

const root = createRoot(document.getElementById('raiz'));
root.render(<App />);
