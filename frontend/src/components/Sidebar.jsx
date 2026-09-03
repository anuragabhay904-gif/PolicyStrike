import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Coins, Receipt, History, HelpCircle } from 'lucide-react';

export const Sidebar = ({ activeOpportunitiesCount }) => {
  const navItems = [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard },
    {
      to: '/opportunities',
      label: 'Opportunities',
      icon: Coins,
      badge: activeOpportunitiesCount > 0 ? activeOpportunitiesCount : null,
    },
    { to: '/transactions', label: 'Transactions', icon: Receipt },
    { to: '/activity', label: 'Audit Trail', icon: History },
  ];

  return (
    <aside className="w-64 border-r border-slate-800 bg-slate-950/40 p-4 flex flex-col justify-between shrink-0 min-h-[calc(100vh-4rem)]">
      <div className="space-y-6">
        <div>
          <div className="px-3 mb-2 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
            Revenue Recovery
          </div>
          <nav className="space-y-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === '/'}
                  className={({ isActive }) =>
                    `flex items-center justify-between px-3 py-2.5 rounded-xl text-sm font-medium transition-all ${
                      isActive
                        ? 'bg-teal-500/10 text-teal-400 border border-teal-500/20 shadow-sm'
                        : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
                    }`
                  }
                >
                  <div className="flex items-center gap-3">
                    <Icon className="w-4 h-4" />
                    <span>{item.label}</span>
                  </div>
                  {item.badge && (
                    <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-amber-500/20 px-1.5 text-xs font-bold text-amber-400 border border-amber-500/30">
                      {item.badge}
                    </span>
                  )}
                </NavLink>
              );
            })}
          </nav>
        </div>

        {/* Leak Categories Info Box */}
        <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800/80 text-xs text-slate-400 space-y-2">
          <div className="font-semibold text-slate-300 flex items-center gap-1.5">
            <span className="h-1.5 w-1.5 rounded-full bg-teal-400"></span>
            <span>Active Leak Scanners</span>
          </div>
          <ul className="space-y-1.5 text-[11px] text-slate-400 pl-2 border-l border-slate-800">
            <li>Failed Payment Recovery</li>
            <li>High-Value Abandoned Cart</li>
            <li>Dormant Repeat Customers</li>
          </ul>
        </div>
      </div>

      <div className="pt-4 border-t border-slate-800 text-[11px] text-slate-500 flex items-center justify-between">
        <span>MVP v1.0.0</span>
        <span className="text-teal-500 font-mono">Razorpay Test</span>
      </div>
    </aside>
  );
};
