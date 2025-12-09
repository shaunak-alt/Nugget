"use client";

import { useMemo, useState } from "react";
import { AlertsList } from "../components/dashboard/AlertsList";
import { IssuesList } from "../components/dashboard/IssuesList";
import { LogTable } from "../components/dashboard/LogTable";
import { PerformanceInsights } from "../components/dashboard/PerformanceInsights";
import { SummaryGrid } from "../components/dashboard/SummaryGrid";
import { TimeRangeKey, TimeRangeSelector } from "../components/dashboard/TimeRangeSelector";
import { NavBar } from "../components/layout/NavBar";
import { AuthGuard } from "../hooks/useAuth";

const RANGE_TO_MS: Record<TimeRangeKey, number> = {
  "1H": 60 * 60 * 1000,
  "24H": 24 * 60 * 60 * 1000,
  "7D": 7 * 24 * 60 * 60 * 1000,
};

export default function DashboardPage() {
  const [range, setRange] = useState<TimeRangeKey>("1H");

  const { from, to } = useMemo(() => {
    const now = new Date();
    const toIso = now.toISOString();
    const duration = RANGE_TO_MS[range];
    const fromDate = new Date(now.getTime() - duration);
    return { from: fromDate.toISOString(), to: toIso };
  }, [range]);

  return (
    <AuthGuard>
      <div className="min-h-screen bg-slate-950">
        <NavBar />
        <main className="mx-auto flex max-w-6xl flex-col gap-6 px-6 py-8">
          <section className="flex flex-col gap-4">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <h2 className="text-lg font-semibold text-slate-100">Overview</h2>
              <TimeRangeSelector value={range} onChange={setRange} />
            </div>
            <SummaryGrid from={from} to={to} />
          </section>
          <LogTable from={from} to={to} />
          <section className="grid gap-6 lg:grid-cols-2">
            <AlertsList />
            <IssuesList />
          </section>
          <PerformanceInsights from={from} to={to} />
        </main>
      </div>
    </AuthGuard>
  );
}
