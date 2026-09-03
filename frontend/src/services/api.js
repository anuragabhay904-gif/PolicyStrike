import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || (import.meta.env.DEV ? 'http://localhost:8080' : '');

const client = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const api = {
  // Dashboard
  getDashboard: async () => {
    const res = await client.get('/api/dashboard');
    return res.data;
  },

  // Opportunities
  getOpportunities: async (status) => {
    const res = await client.get('/api/opportunities', {
      params: status && status !== 'ALL' ? { status } : {},
    });
    return res.data;
  },

  getOpportunity: async (id) => {
    const res = await client.get(`/api/opportunities/${id}`);
    return res.data;
  },

  analyzeOpportunity: async (id) => {
    const res = await client.post(`/api/opportunities/${id}/analyze`);
    return res.data;
  },

  approveOpportunity: async (id) => {
    const res = await client.post(`/api/opportunities/${id}/approve`);
    return res.data;
  },

  dismissOpportunity: async (id) => {
    const res = await client.post(`/api/opportunities/${id}/dismiss`);
    return res.data;
  },

  triggerDetection: async () => {
    const res = await client.post('/api/opportunities/detect');
    return res.data;
  },

  // Audit
  getAuditEvents: async (opportunityId) => {
    const res = await client.get('/api/audit', {
      params: opportunityId ? { opportunityId } : {},
    });
    return res.data;
  },

  // Transactions
  getTransactions: async (params) => {
    const res = await client.get('/api/transactions', { params });
    return res.data;
  },

  // Customers
  getCustomers: async () => {
    const res = await client.get('/api/customers');
    return res.data;
  },

  // Demo Controls
  seedDemoData: async () => {
    const res = await client.post('/api/demo/seed');
    return res.data;
  },

  simulateLeak: async (payload = {}) => {
    const res = await client.post('/api/demo/simulate-leak', payload);
    return res.data;
  },

  toggleFailureSimulation: async (enabled) => {
    const res = await client.post('/api/demo/simulate-failure', null, {
      params: enabled !== undefined ? { enabled } : {},
    });
    return res.data;
  },

  getDemoStatus: async () => {
    const res = await client.get('/api/demo/status');
    return res.data;
  },

  // Health
  getHealth: async () => {
    const res = await client.get('/api/health');
    return res.data;
  },
};

export default api;
